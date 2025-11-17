using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class RotateObject : MonoBehaviour
{
    public float rotationSpeed = 5f;
    public Space rotationSpace = Space.Self;

    void Update()
    {
        transform.Rotate(Vector3.up, -rotationSpeed * Time.deltaTime, rotationSpace);
    }
}