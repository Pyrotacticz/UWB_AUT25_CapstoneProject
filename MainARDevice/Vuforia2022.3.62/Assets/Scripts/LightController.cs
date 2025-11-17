using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

public class LightController : MonoBehaviour
{
    public Transform LightLocation;
    public Transform switchedLightLocation;
    public Slider sliderX;
    public Slider sliderY;
    public Slider sliderZ;
    public TextMeshProUGUI textX;
    public TextMeshProUGUI textY;
    public TextMeshProUGUI textZ;
    public Toggle toggleLight;

    // Start is called before the first frame update
    void Start()
    {
        Transform chosenLightLocation = CheckActiveDirectionalLight();
        sliderX.minValue = chosenLightLocation.localPosition.x - 20;
        sliderX.maxValue = chosenLightLocation.localPosition.x + 20;
        sliderX.value = chosenLightLocation.localPosition.x;
        sliderX.onValueChanged.AddListener(OnSliderXValueChanged);
        textX.text = chosenLightLocation.localPosition.x.ToString();

        sliderY.minValue = chosenLightLocation.localPosition.y - 20;
        sliderY.maxValue = chosenLightLocation.localPosition.y + 20;
        sliderY.value = chosenLightLocation.localPosition.y;
        sliderY.onValueChanged.AddListener(OnSliderYValueChanged);
        textY.text = chosenLightLocation.localPosition.y.ToString();

        sliderZ.minValue = chosenLightLocation.localPosition.z - 20;
        sliderZ.maxValue = chosenLightLocation.localPosition.z + 20;
        sliderZ.value = chosenLightLocation.localPosition.z;
        sliderZ.onValueChanged.AddListener(OnSliderZValueChanged);
        textZ.text = chosenLightLocation.localPosition.z.ToString();

        toggleLight.onValueChanged.AddListener(OnToggleChanged);
    }

    void OnSliderXValueChanged(float value)
    {
        Transform chosenLightLocation = CheckActiveDirectionalLight();
        chosenLightLocation.localPosition = new Vector3(sliderX.value, chosenLightLocation.localPosition.y, chosenLightLocation.localPosition.z);
        textX.text = LightLocation.localPosition.x.ToString();
    }

    void OnSliderYValueChanged(float value)
    {
        Transform chosenLightLocation = CheckActiveDirectionalLight();
        chosenLightLocation.localPosition = new Vector3(chosenLightLocation.localPosition.x, sliderY.value, chosenLightLocation.localPosition.z);
        textY.text = LightLocation.localPosition.y.ToString();
    }

    void OnSliderZValueChanged(float value)
    {
        Transform chosenLightLocation = CheckActiveDirectionalLight();
        chosenLightLocation.localPosition = new Vector3(chosenLightLocation.localPosition.x, chosenLightLocation.localPosition.y, sliderZ.value);
        textZ.text = chosenLightLocation.localPosition.z.ToString();
    }

    public void UpdateVuforialocalPositioning()
    {
        Transform chosenLightLocation = CheckActiveDirectionalLight();
        sliderX.minValue = chosenLightLocation.localPosition.x - 20;
        sliderX.maxValue = chosenLightLocation.localPosition.x + 20;
        sliderX.value = chosenLightLocation.localPosition.x;
        textX.text = chosenLightLocation.localPosition.x.ToString();

        sliderY.minValue = chosenLightLocation.localPosition.y - 20;
        sliderY.maxValue = chosenLightLocation.localPosition.y + 20;
        sliderY.value = chosenLightLocation.localPosition.y;
        textY.text = chosenLightLocation.localPosition.y.ToString();

        sliderZ.minValue = chosenLightLocation.localPosition.z - 20;
        sliderZ.maxValue = chosenLightLocation.localPosition.z + 20;
        sliderZ.value = chosenLightLocation.localPosition.z;
        textZ.text = chosenLightLocation.localPosition.z.ToString();
    }

    public void CheckLocation()
    {
        textX.text = LightLocation.localPosition.x.ToString();
        textY.text = LightLocation.localPosition.y.ToString();
        textZ.text = LightLocation.localPosition.z.ToString();
    }

    void OnToggleChanged(bool flipped)
    {
        LightLocation.gameObject.SetActive(!flipped);
        switchedLightLocation.gameObject.SetActive(flipped);
        //UpdateVuforialocalPositioning();
    }

    Transform CheckActiveDirectionalLight()
    {
        return switchedLightLocation;
    }
}
