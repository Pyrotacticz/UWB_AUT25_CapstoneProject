using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ReflectProbeRefreshRate : MonoBehaviour
{
    public ReflectionProbe reflectionProbe;
    public int frameCount = 1;

    // Start is called before the first frame update
    void Start()
    {
        if (frameCount <= 0)
        {
            frameCount = 1;
        }
        StartCoroutine(RefreshLoopRoutine());
    }

    // Update is called once per frame
    void Update()
    {
        
    }

    private IEnumerator RefreshLoopRoutine()
    {
        while(true)
        {
            for (int i = 0; i < frameCount; i++)
            {
                yield return null;
            }
            reflectionProbe.RenderProbe();
        }
    }
}
