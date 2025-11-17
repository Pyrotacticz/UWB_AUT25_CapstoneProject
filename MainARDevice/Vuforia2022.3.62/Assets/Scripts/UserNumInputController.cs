using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

public class UserNumInputController : MonoBehaviour
{
    public TMP_InputField offsetX;
    public TMP_InputField offsetZ;
    public TMP_InputField inputX;
    public TMP_InputField inputY;
    public TMP_InputField inputZ;
    private static float CM_TO_M = 100;


    // Start is called before the first frame update
    void Start()
    {
        offsetX.onEndEdit.AddListener(OnInputFieldEndEditOffsetX);
        offsetZ.onEndEdit.AddListener(OnInputFieldEndEditOffsetZ);

        inputX.onEndEdit.AddListener(OnInputFieldEndEditInputX);
        inputY.onEndEdit.AddListener(OnInputFieldEndEditInputY);
        inputZ.onEndEdit.AddListener(OnInputFieldEndEditInputZ);
    }

    // Update is called once per frame
    void Update()
    {
        
    }


    void OnInputFieldEndEditOffsetX(string finalText)
    {
        float res;
        float.TryParse(finalText, out res);
        res = res != 0 ? res / CM_TO_M : res;
        ConfigDataForAR.OffsetX = res;
    }

    void OnInputFieldEndEditOffsetZ(string finalText)
    {
        float res;
        float.TryParse(finalText, out res);
        res = res != 0 ? res / CM_TO_M : res;
        ConfigDataForAR.OffsetZ = res;
    }

    void OnInputFieldEndEditInputX(string finalText)
    {
        float res;
        float.TryParse(finalText, out res);
        res = res != 0 ? res / CM_TO_M : res;
        ConfigDataForAR.DefaultX = res;
    }

    void OnInputFieldEndEditInputY(string finalText)
    {
        float res;
        float.TryParse(finalText, out res);
        res = res != 0 ? res / CM_TO_M : res;
        ConfigDataForAR.DefaultY = res;
    }

    void OnInputFieldEndEditInputZ(string finalText)
    {
        float res;
        float.TryParse(finalText, out res);
        res = res != 0 ? res / CM_TO_M : res;
        ConfigDataForAR.DefaultZ = res;
    }
}
