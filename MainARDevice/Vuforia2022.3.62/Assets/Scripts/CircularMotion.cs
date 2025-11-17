using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class CircularMotion : MonoBehaviour
{
    public Transform FrontRightWheel;
    public Transform FrontLeftWheel;
    public Transform BackRightWheel;
    public Transform BackLeftWheel;
    //public float circleRadius = 5f; // Radius of the circle
    public float rotationSpeed = 1f; // Speed of rotation

    private float currentAngle;

    void Update()
    {
        // Increase the angle over time
        currentAngle = rotationSpeed;

        // Calculate the car's position using trigonometry
        //float x = Mathf.Cos(currentAngle) * circleRadius;
        //float z = Mathf.Sin(currentAngle) * circleRadius; // Or Y for 2D

        // Set the car's position
        //transform.position = new Vector3(x, transform.position.y, z);

        // Rotate The Wheels
        UpdateWheels(currentAngle);

        // Optionally, update the car's rotation to face the direction of movement
        //transform.LookAt(transform.position + new Vector3(Mathf.Cos(currentAngle + Mathf.PI / 2f), 0, Mathf.Sin(currentAngle + Mathf.PI / 2f)));
    }

    void UpdateWheels(float rotation)
    {
        FrontLeftWheel.Rotate(rotation, 0, 0);
        FrontRightWheel.Rotate(-rotation, 0, 0);
        BackLeftWheel.Rotate(rotation, 0, 0);
        BackRightWheel.Rotate(-rotation, 0, 0);
    }
}
