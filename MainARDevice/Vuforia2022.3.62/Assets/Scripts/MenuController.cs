using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;
using TMPro;
using System.Collections;


public class MenuScript : MonoBehaviour
{
    public Toggle toggleCar;
    public Toggle toggleRobot;
    public Toggle togglePen;
    public Button calibrationButton;
    public Button arButton;

    // Start is called once before the first execution of Update after the MonoBehaviour is created
    void Start()
    {
        toggleCar.onValueChanged.AddListener(UpdateCarSpawn);
        toggleRobot.onValueChanged.AddListener(UpdateRobotSpawn);
        togglePen.onValueChanged.AddListener(UpdatePenSpawn);
        //arButton.onClick.AddListener(LoadARScene);
    }

    // Update is called once per frame
    void Update()
    {
        
    }

    public void LoadARScene()
    {
        Debug.Log("AR Button Clicked");
        //InitializeLightSourcePosition();
        SceneManager.LoadScene("VuforiaScene");
    }

    public void ExitApp()
    {
        Application.Quit();
        Debug.Log("User exited the application");
    }

    private void UpdateCarSpawn(bool toggle)
    {
        ConfigDataForAR.SpawnCar = toggle;
    }

    private void UpdateRobotSpawn(bool toggle)
    {
        ConfigDataForAR.SpawnRobot = toggle;
    }

    private void UpdatePenSpawn(bool toggle)
    {
        ConfigDataForAR.SpawnPen = toggle;
    }

}


