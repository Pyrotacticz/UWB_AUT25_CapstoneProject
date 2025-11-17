using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using Vuforia;
using Image = Vuforia.Image;


// taken from Vuforia website as example with my modifications
public class CameraImageAccess : MonoBehaviour
{
    const PixelFormat PIXEL_FORMAT = PixelFormat.RGBA8888;
    const TextureFormat TEXTURE_FORMAT = TextureFormat.RGBA32;

    public RawImage RawImage;
    public Material camBox;
    private Material orgBox;
    public Material bot;
    public GameObject surface;

    Texture2D mTexture;
    bool mFormatRegistered;
    bool singleUpdate;

    void Start()
    {
        // Register Vuforia Engine life-cycle callbacks:
        VuforiaApplication.Instance.OnVuforiaStarted += OnVuforiaStarted;
        VuforiaApplication.Instance.OnVuforiaStopped += OnVuforiaStopped;
        if (VuforiaBehaviour.Instance != null)
            VuforiaBehaviour.Instance.World.OnStateUpdated += OnVuforiaUpdated;
        orgBox = RenderSettings.skybox;
    }

    void OnDestroy()
    {
        // Unregister Vuforia Engine life-cycle callbacks:
        if (VuforiaBehaviour.Instance != null)
            VuforiaBehaviour.Instance.World.OnStateUpdated -= OnVuforiaUpdated;

        VuforiaApplication.Instance.OnVuforiaStarted -= OnVuforiaStarted;
        VuforiaApplication.Instance.OnVuforiaStopped -= OnVuforiaStopped;

        if (VuforiaApplication.Instance.IsRunning)
        {
            // If Vuforia Engine is still running, unregister the camera pixel format to avoid unnecessary overhead
            // Formats can only be registered and unregistered while Vuforia Engine is running
            UnregisterFormat();
        }

        RenderSettings.skybox = orgBox;
        if (mTexture != null)
            Destroy(mTexture);
    }

    /// 
    /// Called each time the Vuforia Engine is started
    /// 
    void OnVuforiaStarted()
    {
        mTexture = new Texture2D(0, 0, TEXTURE_FORMAT, false);
        // A format cannot be registered if Vuforia Engine is not running
        RegisterFormat();
    }

    /// 
    /// Called each time the Vuforia Engine is stopped
    /// 
    void OnVuforiaStopped()
    {
        // A format cannot be unregistered after OnVuforiaStopped
        UnregisterFormat();
        if (mTexture != null)
            Destroy(mTexture);
    }

    /// 
    /// Called each time the Vuforia Engine state is updated
    /// 
    void OnVuforiaUpdated()
    {
        var image = VuforiaBehaviour.Instance.CameraDevice.GetCameraImage(PIXEL_FORMAT);

        // There can be a delay of several frames until the camera image becomes available
        if (Image.IsNullOrEmpty(image))
            return;
        /*
        Debug.Log("\nImage Format: " + image.PixelFormat +
                  "\nImage Size: " + image.Width + " x " + image.Height +
                  "\nBuffer Size: " + image.BufferWidth + " x " + image.BufferHeight +
                  "\nImage Stride: " + image.Stride + "\n");
        */

        if (singleUpdate)
            return;
        // Override the current texture by copying into it the camera image flipped on the Y axis
        // The texture is resized to match the camera image size
        image.CopyToTexture(mTexture, false); // false means no flip on Y

        if (Screen.orientation == ScreenOrientation.Portrait)
        {
            mTexture = rotateTexture(mTexture, true);
        }
        //RawImage.texture = mTexture;
        //RawImage.material.mainTexture = mTexture;
        //camBox.mainTexture = mTexture;
        //RenderSettings.skybox = camBox;
        //DynamicGI.UpdateEnvironment();

        AssignSingleTextureToSkyBoxMaterial(mTexture, camBox);
        singleUpdate = true;
    }

    /// 
    /// Register the camera pixel format
    /// 
    void RegisterFormat()
    {
        // Vuforia Engine has started, now register camera image format
        var success = VuforiaBehaviour.Instance.CameraDevice.SetFrameFormat(PIXEL_FORMAT, true);
        if (success)
        {
            Debug.Log("Successfully registered pixel format " + PIXEL_FORMAT);
            mFormatRegistered = true;
        }
        else
        {
            Debug.LogError("Failed to register pixel format " + PIXEL_FORMAT +
                           "\n the format may be unsupported by your device;" +
                           "\n consider using a different pixel format.");
            mFormatRegistered = false;
        }
    }

    /// 
    /// Unregister the camera pixel format
    /// 
    void UnregisterFormat()
    {
        Debug.Log("Unregistering camera pixel format " + PIXEL_FORMAT);
        VuforiaBehaviour.Instance.CameraDevice.SetFrameFormat(PIXEL_FORMAT, false);
        mFormatRegistered = false;
    }

    //https://discussions.unity.com/t/rotate-the-contents-of-a-texture/136686
    Texture2D rotateTexture(Texture2D originalTexture, bool clockwise)
    {
        Color32[] original = originalTexture.GetPixels32();
        Color32[] rotated = new Color32[original.Length];
        int w = originalTexture.width;
        int h = originalTexture.height;

        int iRotated, iOriginal;

        for (int j = 0; j < h; ++j)
        {
            for (int i = 0; i < w; ++i)
            {
                iRotated = (i + 1) * h - j - 1;
                iOriginal = clockwise ? original.Length - 1 - (j * w + i) : j * w + i;
                rotated[iRotated] = original[iOriginal];
            }
        }

        Texture2D rotatedTexture = new Texture2D(h, w);
        rotatedTexture.SetPixels32(rotated);
        rotatedTexture.Apply();
        return rotatedTexture;
    }

    
    private Texture2D[] SplitUpSingleTextureIntoSixTextures(Texture2D tex)
    {
        Texture2D[] textures = new Texture2D[6];
        int height = tex.height;
        int width = tex.width;
        int n = height > width ? width : height;
        int section = n / 3;
        int centerU = width / 2;
        int centerV = height / 2;

        int offset = section / 2;
        int u = centerU - offset;
        int v = centerV - offset;

        Texture2D texture = CreateTextureSection(tex, u, v, section);
        textures[0] = texture;
        texture = CreateTextureSection(tex, u + section, v, section);
        textures[2] = texture;
        texture = CreateTextureSection(tex, u - section, v, section);
        textures[3] = texture;
        texture = CreateTextureSection(tex, u, v - section, section);
        textures[5] = texture;

        return textures;
    }

    private Texture2D CreateTextureSection(Texture2D tex, int originU, int originV, int size)
    {
        Color[] subPixels = tex.GetPixels(
            originU,
            originV,
            size,
            size
        );

        Texture2D newTexture = new Texture2D(size, size);
        newTexture.SetPixels(subPixels);
        newTexture.Apply();

        return newTexture;
    }

    private void AssignSingleTextureToSkyBoxMaterial(Texture2D tex, Material skyboxMaterial)
    {
        Texture2D[] textures = SplitUpSingleTextureIntoSixTextures(tex);
        if (skyboxMaterial == null)
        {
            Debug.LogError("Skybox Material not assigned!");
            return;
        }
        skyboxMaterial.SetTexture("_FrontTex", textures[0]);
        //skyboxMaterial.SetTexture("_BackTex", textures[1]);
        skyboxMaterial.SetTexture("_LeftTex", textures[2]);
        skyboxMaterial.SetTexture("_RightTex", textures[3]);
        //skyboxMaterial.SetTexture("_UpTex", textures[4]);
        skyboxMaterial.SetTexture("_DownTex", textures[5]);

        // Update shadows with tint of surface
        Renderer renderer = surface.GetComponent<Renderer>();
        Material mat = renderer.material;
        mat.SetColor("_Surface_Texture_Color", SampleColorFromTexture(textures[5]));

        RenderSettings.skybox = skyboxMaterial;
        DynamicGI.UpdateEnvironment();

    }

    public void UpdateReflectionTextureOnReposition()
    {
        singleUpdate = false;
    }

    private Color SampleColorFromTexture(Texture2D texture)
    {
        int height = texture.height;
        int width = texture.width;
        int n = height > width ? width : height;
        int centerU = width / 2;
        int centerV = height / 2;
        var coords = new (int u, int v)[] {
            (centerU, centerV), (centerU - n / 2, centerV), (centerU + n / 2, centerV),
            (centerU, centerV - n / 2), (centerU, centerV + n / 2), (centerU + n / 2, centerV + n / 2),
            (centerU + n / 2, centerV - n / 2), (centerU - n / 2, centerV - n / 2), (centerU - n / 2, centerV + n / 2)
        };
        Vector4 sum = Vector4.zero;
        foreach (var coord in coords)
        {
            Color c = texture.GetPixel(coord.u, coord.v);
            sum += new Vector4(c.r, c.g, c.b, c.a);
        }

        sum /= coords.Length;

        return new Color(sum.x, sum.y, sum.z, sum.w);
    }
}