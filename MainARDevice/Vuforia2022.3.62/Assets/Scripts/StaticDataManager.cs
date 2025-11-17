using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class StaticDataManager : MonoBehaviour
{
    public GameObject directionalLight;
    public GameObject measuredLight;
    public GameObject car;
    public GameObject robot;
    public GameObject pen;

    // Start is called before the first frame update
    void Start()
    {
        car.SetActive(ConfigDataForAR.SpawnCar);
        robot.SetActive(ConfigDataForAR.SpawnRobot);
        pen.SetActive(ConfigDataForAR.SpawnPen);
        directionalLight.transform.localPosition = new Vector3(ConfigDataForAR.DefaultX, ConfigDataForAR.DefaultY, ConfigDataForAR.DefaultZ);
        measuredLight.transform.localPosition = new Vector3(ConfigDataForAR.DefaultX, ConfigDataForAR.DefaultY, ConfigDataForAR.DefaultZ);
    }

    // Update is called once per frame
    void Update()
    {
        
    }
}


public static class ConfigDataForAR
{
    public static bool SpawnCar {get; set;} = true;
    public static bool SpawnRobot {get; set;} = true;
    public static bool SpawnPen {get; set;} = true;
    public static float OffsetX {get; set;} = 0.0f;
    public static float OffsetZ {get; set;} = 0.0f;
    public static float DefaultX {get; set;} = 0.0f;
    public static float DefaultY {get; set;} = 4.0f;
    public static float DefaultZ {get; set;} = 0.0f;
}