package com.vegawatt.sensors.simulation;

import java.util.Random;
import org.springframework.stereotype.Component;

@Component
class JdkRandomSource implements RandomSource {

    private final Random random = new Random();

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }
}
