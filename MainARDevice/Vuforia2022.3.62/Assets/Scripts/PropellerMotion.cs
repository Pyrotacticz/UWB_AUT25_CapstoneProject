using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class PropellerMotion : MonoBehaviour
{
    public Transform Propeller;
    public float rotationSpeed = 1f; // Speed of rotation

    void Update()
    {
        Propeller.Rotate(0, rotationSpeed, 0);
    }

}