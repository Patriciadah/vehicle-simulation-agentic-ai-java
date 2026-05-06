package org.example.model;

public class Vector2D {
    public final double x;
    public final double y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }


    public Vector2D add(Vector2D v) {
        return new Vector2D(this.x+v.x,this.y+v.y);
    }

    public Vector2D subtract(Vector2D v) {
        return new Vector2D( this.x -v.x,this.y - v.y);
    }

    public static Vector2D subtract(Vector2D v1, Vector2D v2) {
        return new Vector2D(v1.x - v2.x, v1.y - v2.y);
    }

    public Vector2D multiply(double n) {
        return new Vector2D(this.x * n,
                this.y * n);
    }

    public Vector2D divide(double n) {
        if (n != 0) {
            return new Vector2D(this.x / n,
            this.y / n);
        }
        return copy();
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y);
    }

    public double distance(Vector2D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public Vector2D normalize() {
        double m = magnitude();
        if (m > 0) {
           return divide(m);
        }
        return copy();
    }

    public Vector2D limitMagnitude(double max) {
        if (magnitude() > max) {
            return normalize().multiply(max);
        }
        return copy();
    }

    // Used to clone the vector to avoid reference sharing bugs
    public Vector2D copy() {
        return new Vector2D(this.x, this.y);
    }
}
