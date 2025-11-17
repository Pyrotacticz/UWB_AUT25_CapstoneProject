using UnityEngine;

public class AngleAnchor : MonoBehaviour
{
    public Vector3 angularVelocity;
    public Space space = Space.Self;

    // Update is called once per frame
    void Update()
    {
        //transform.Rotate(angularVelocity * Time.deltaTime, space);
    }
}
