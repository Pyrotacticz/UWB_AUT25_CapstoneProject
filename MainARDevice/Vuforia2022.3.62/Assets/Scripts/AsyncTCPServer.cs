using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;
using UnityEngine;
using SimpleJSON;
using Vuforia;


// taken from chatgpt
public class AsyncTcpServer : MonoBehaviour
{    
    [System.Serializable]
    public class BrightSpot
    {
        public float x;
        public float y;
        public float z;
        public float radius;
        public float meanIntensity;
        public float r;
        public float g;
        public float b;
        public float a;
        private float threshold = 10;

        public bool Equals(BrightSpot other)
        {
            if (other == null) return false;
            return radius - other.radius <= threshold;
        }
    }

    private TcpListener tcpListener;
    private bool isRunning = false;
    private List<KeyValuePair<GameObject, float>> existingLightObjects;


    private ConcurrentQueue<List<BrightSpot>> messageQueue;

    public Transform parent;
    private Vector3 defaultPosition;
    public GameObject directionalLightSource;
    private float directionalRadius;
    private float sceneFactor;

    void Start()
    {
        sceneFactor = VuforiaConfiguration.Instance.Vuforia.VirtualSceneScaleFactor;
        messageQueue = new ConcurrentQueue<List<BrightSpot>>();
        isRunning = true;
        existingLightObjects = new List<KeyValuePair<GameObject, float>>();
        if (directionalLightSource == null)
        {
            throw new Exception("No main directional light established");
        }
        directionalRadius = 0f;
        Vector3 savedPosition = directionalLightSource.transform.localPosition;
        defaultPosition = new Vector3(savedPosition.x, savedPosition.y, savedPosition.z);
        StartListeningAsync(); // Fire-and-forget
    }

    private async void StartListeningAsync()
    {
        tcpListener = new TcpListener(IPAddress.Any, 8052);
        tcpListener.Start();

        string localIP = GetLocalIPAddress();
        Debug.Log("Listening on IP: " + localIP);
        Debug.Log("Unity TCP Server started on port 8052");

        try
        {
            while (isRunning)
            {
                TcpClient client = await tcpListener.AcceptTcpClientAsync();
                _ = HandleClientAsync(client); // Handle without blocking loop
            }
        }
        catch (ObjectDisposedException)
        {
            Debug.Log("Listener was disposed, stopping server.");
        }
        catch (Exception ex)
        {
            Debug.LogError("Listener exception: " + ex.Message);
        }
    }

    private async Task HandleClientAsync(TcpClient client)
    {
        try
        {
            using (var stream = client.GetStream())
            {
                byte[] buffer = new byte[1024];
                int bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length);
                string jsonString = Encoding.ASCII.GetString(buffer, 0, bytesRead);

                Debug.Log("Received JSON: " + jsonString);

                var parsed = JSON.Parse(jsonString);

                if (parsed == null || !parsed.HasKey("lights"))
                {
                    Debug.LogWarning("Invalid JSON or missing 'lights' key.");
                    return;
                }

                var lightsArray = parsed["lights"].AsArray;
                List<BrightSpot> lights = new List<BrightSpot>();
                foreach (JSONNode lightNode in lightsArray)
                {
                    BrightSpot light = new BrightSpot
                    {
                        x = lightNode["x"].AsFloat,
                        y = lightNode["y"].AsFloat,
                        z = lightNode["z"].AsFloat,
                        radius = lightNode["radius"].AsFloat,
                        meanIntensity = lightNode["mean_intensity"].AsFloat,
                        r = lightNode["r"].AsFloat / 255f,
                        g = lightNode["g"].AsFloat / 255f,
                        b = lightNode["b"].AsFloat / 255f,
                        a = lightNode["a"].AsFloat / 255f
                    };
                    Debug.Log($"Light at ({light.x}, {light.y}, {light.z}) with color ({light.r}, {light.g}, {light.b}, {light.a})");
                    // You can now spawn lights, set materials, etc., using this data

                    lights.Add(light);
                }
                messageQueue.Enqueue(lights);


                string response = "Message received";
                byte[] responseBytes = Encoding.ASCII.GetBytes(response);
                await stream.WriteAsync(responseBytes, 0, responseBytes.Length);
            }

            client.Close();
        }
        catch (Exception e)
        {
            Debug.LogError("Client handler error: " + e.Message);
        }
    }


    void Update()
    {
        while (messageQueue.TryDequeue(out var data))
        {
            GenerateLightSources(data);
        }
    }

    void OnApplicationQuit()
    {
        isRunning = false;
        tcpListener?.Stop();
    }

    string GetLocalIPAddress()
    {
        var host = Dns.GetHostEntry(Dns.GetHostName());
        foreach (var ip in host.AddressList)
        {
            if (ip.AddressFamily == AddressFamily.InterNetwork)
            {
                return ip.ToString();
            }
        }
        return "No network adapters with an IPv4 address found.";
    }

    void GenerateLightSources(List<BrightSpot> data)
    {
        if (data.Count == 0)
        {
            ModifyMainLightSource(null);
        }
        else if (data.Count >= 1)
        {
            BrightSpot incomingSpot = data[0];
            data.RemoveAt(0);
            ModifyMainLightSource(incomingSpot);
            if (data.Count == 0)
            {
                return;
            }
        }
        //return;
        for (int i = 0; i < data.Count; i++)
        {
            BrightSpot incomingSpot = data[i];
            if (i < existingLightObjects.Count) // update the old ones
            {
                float radius = existingLightObjects[i].Value;
                GameObject existingLightObject = existingLightObjects[i].Key;
                bool areTheLightsDifferent = CompareIncomingLightSource(incomingSpot, radius, existingLightObject);
                if (areTheLightsDifferent)
                {
                    UpdateExistingLightSource(existingLightObject, incomingSpot);
                    existingLightObjects[i] = new KeyValuePair<GameObject, float>(existingLightObjects[i].Key, incomingSpot.radius);
                }
            }
            else // new lights
            {
                GameObject lightSource = CreatePointLightSource(incomingSpot);
                existingLightObjects.Add(new KeyValuePair<GameObject, float>(lightSource, incomingSpot.radius));
            }
        }
        for (int i = existingLightObjects.Count - 1; i >= data.Count; i--)
        {
            GameObject lightSource = existingLightObjects[i].Key;
            if (lightSource != null)
            {
                existingLightObjects.RemoveAt(i);
                UnityEngine.Object.Destroy(lightSource);
            }
        }
    }

    void ModifyMainLightSource(BrightSpot incomingSpot)
    {
        if (incomingSpot == null)
        {
            directionalLightSource.transform.localPosition = defaultPosition;
            directionalLightSource.transform.localRotation = Quaternion.Euler(90f, 0f, 0f);
            directionalRadius = 0f;
        }
        else
        {
            float radius = directionalRadius;
            GameObject existingLightObject = directionalLightSource;
            bool areTheLightsDifferent = CompareIncomingLightSource(incomingSpot, radius, existingLightObject);
            if (areTheLightsDifferent)
            {
                UpdateExistingDirectionalLightSource(existingLightObject, incomingSpot);
                directionalLightSource = existingLightObject;
                directionalRadius = incomingSpot.radius;
            }
        }
    }

    GameObject CreatePointLightSource(BrightSpot data)
    {
        GameObject lightSource = new GameObject("Lightsource");
        lightSource.transform.SetParent(parent);
        Light lightComponent = lightSource.AddComponent<Light>();
        lightComponent.type = LightType.Point;
        lightComponent.shadows = LightShadows.Soft;
        //lightComponent.range = Mathf.Clamp(1.5f * Mathf.Sqrt(data.meanIntensity), 20f, 80f);
        Color estimatedLightColor = new Color(data.r, data.g, data.b, data.a);
        lightComponent.color = estimatedLightColor;
        lightComponent.intensity = NormalizeIntensityPointLight(data.meanIntensity);
        float includeOffsetX = data.x + ConfigDataForAR.OffsetX;
        float includeOffsetZ = data.z + ConfigDataForAR.OffsetZ;
        lightSource.transform.localPosition = new Vector3(includeOffsetX, data.y, includeOffsetZ) * sceneFactor / 2;
        lightComponent.range = Mathf.Max(includeOffsetX, data.y, includeOffsetZ) * sceneFactor;
        return lightSource;
    }


    void UpdateExistingLightSource(GameObject obj, BrightSpot data)
    {
        Light lightComponent = obj.GetComponent<Light>();
        Color estimatedLightColor = new Color(data.r, data.g, data.b, data.a);
        lightComponent.color = estimatedLightColor;
        lightComponent.intensity = NormalizeIntensityPointLight(data.meanIntensity);
        float includeOffsetX = data.x + ConfigDataForAR.OffsetX;
        float includeOffsetZ = data.z + ConfigDataForAR.OffsetZ;
        obj.transform.localPosition = new Vector3(includeOffsetX, data.y, includeOffsetZ) * sceneFactor / 2;
        lightComponent.range = Mathf.Max(includeOffsetX, data.y, includeOffsetZ) * sceneFactor;
    }


    void UpdateExistingDirectionalLightSource(GameObject obj, BrightSpot data)
    {
        Light lightComponent = obj.GetComponent<Light>();
        Color estimatedLightColor = new Color(data.r, data.g, data.b, data.a);
        lightComponent.color = estimatedLightColor;
        lightComponent.intensity = NormalizeIntensityDirectional(data.meanIntensity);
        float includeOffsetX = data.x + ConfigDataForAR.OffsetX;
        float includeOffsetZ = data.z + ConfigDataForAR.OffsetZ;
        obj.transform.localPosition = new Vector3(includeOffsetX, data.y, includeOffsetZ) * sceneFactor;
    }


    // TODO FIX THIS - HOW TO UPDATE A SINGLE LIGHT THAT MOVES WITH SAME RADIUS
    private float threshold = 10f;
    bool CompareIncomingLightSource(BrightSpot incoming, float radius, GameObject existing)
    {
        Light existingLight = existing.GetComponent<Light>();
        float incomingIntensity = 0;
        float factor = sceneFactor;
        if (existingLight.type == LightType.Point)
        {
            incomingIntensity = NormalizeIntensityPointLight(incoming.meanIntensity);
            factor /= 2;
        }
        else
        {
            incomingIntensity = NormalizeIntensityDirectional(incoming.meanIntensity);
        }

        bool checkRadius = Mathf.Abs(incoming.radius - radius) > threshold;
        Vector3 existingPosition = existing.transform.localPosition;
        float incomingX = incoming.x + ConfigDataForAR.OffsetX;
        float incomingY = incoming.y;
        float incomingZ = incoming.z + ConfigDataForAR.OffsetZ;
        bool checkX = Mathf.Abs(incomingX * factor - existingPosition.x) > threshold;
        bool checkY = Mathf.Abs(incomingY * factor - existingPosition.y) > threshold;
        bool checkZ = Mathf.Abs(incomingZ * factor - existingPosition.z) > threshold;

        bool checkR = Mathf.Abs(incoming.r - existingLight.color.r) > threshold / 255f;
        bool checkG = Mathf.Abs(incoming.g - existingLight.color.g) > threshold / 255f;
        bool checkB = Mathf.Abs(incoming.b - existingLight.color.b) > threshold / 255f;

        bool checkIntensity = Mathf.Abs(incomingIntensity - existingLight.intensity) > threshold;

        Debug.Log(string.Format("diff in radius: {0}, position: {1}, color: {2}, intensity {3}",
        checkRadius, checkX && checkY && checkZ, checkR && checkG && checkB, checkIntensity));

        return checkRadius || (checkX || checkY || checkZ) || (checkR || checkG || checkB) || checkIntensity;
    }

    // TODO - FIX CLAMP FOR POINT LIGHT AND DIRECTIONAL LIGHT
    // TODO - FIX INTENSITY SCALE (DIFFERS FOR POINT LIGHT AND DIRECTIONAL LIGHT)
    float NormalizeIntensity(float intensity)
    {
        float normalized = intensity / 255.0f;
        float linear = Mathf.Pow(normalized, 2.2f); // gamma correction
        float scaled = Mathf.Clamp(linear * 2f, 0f, 4f);
        Debug.Log(scaled);
        return scaled;
    }

    
    float NormalizeIntensityDirectional(float intensity)
    {
        float normalized = intensity / 255.0f * 0.7f;
        return normalized;
    }

    float NormalizeIntensityPointLight(float intensity)
    {
        float normalized = intensity / 10.0f;
        return normalized;
    }
}
