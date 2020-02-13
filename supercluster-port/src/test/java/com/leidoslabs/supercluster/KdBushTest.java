package com.leidoslabs.supercluster;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.function.Function;

import com.leidoslabs.util.KDBush;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;

public class KdBushTest {

    private static final double DELTA = 1e-15;
    
    private static Coordinate[] coords = {
        new Coordinate(0, 0),
        new Coordinate(1, 1),
        new Coordinate(2, 2),
        new Coordinate(3, 3),
        new Coordinate(4, 4)
    };
    
    @Test
    public void kdbushRange() {
    
        KDBush<Coordinate> bush = new KDBush<>(coords, new GetCoordinate());
    
        List<Coordinate> nodes = bush.range(1, 1, 3, 3); //should have (1,1) (2,2) (3,3)
    
        assertEquals(nodes.size(), 3);
    
        for(int i = 1; i <= 3; i++) {
            
            Coordinate coord = nodes.get(i - 1);
            
            //delta = amount of difference for the two nums to be considered equal (since they are doubles)
            assertEquals(coord.getX(), i, DELTA);
            assertEquals(coord.getY(), i, DELTA);
        }
    }
    
    @Test
    public void kdbushWithin() {
    
        KDBush<Coordinate> bush = new KDBush<>(coords, new GetCoordinate());
        Coordinate coord;

        List<Coordinate> nodes = bush.within(2, 2, 1.5); // should have (1,1) (2,2) (3,3)
        
        assertEquals(nodes.size(), 3);

        for(int i = 1; i <= 3; i++) {

            coord = nodes.get(i - 1);

            assertEquals(coord.getX(), i, DELTA);
            assertEquals(coord.getY(), i, DELTA);

        }

        List<Coordinate> nodes2 = bush.within(-1, -1, 2); //should have (0, 0)
        assertEquals(nodes2.size(), 1);
        coord = nodes2.get(0);
        
        assertEquals(coord.getX(), 0, DELTA);
        assertEquals(coord.getY(), 0, DELTA);
    }

    @Test
    public void kdbushGetPoints() {

        KDBush<Coordinate> bush = new KDBush<>(coords, new GetCoordinate());
        List<Coordinate> nodes = bush.getPoints();

        for(int i = 0; i < 5; i++) {

            Coordinate coord = nodes.get(i - 0);

            assertEquals(coord.getX(), i, DELTA);
            assertEquals(coord.getY(), i, DELTA);
        }
    }

    private class GetCoordinate implements Function<Coordinate, Coordinate> {
        @Override
        public Coordinate apply(Coordinate t) {
            return t;
        }
    }
}

