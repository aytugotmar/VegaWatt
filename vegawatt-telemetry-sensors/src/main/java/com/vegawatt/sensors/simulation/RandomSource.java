package com.vegawatt.sensors.simulation;

public interface RandomSource {

    /**
     * @return a value in [0.0, 1.0)
     */
    double nextDouble();
}
