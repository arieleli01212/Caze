package com.hit.dm;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@link Campus} aggregate.
 */
public class CampusTest {

    @Test
    public void testNameAndIdLookups() {
        Campus campus = sampleCampus();
        assertEquals(Integer.valueOf(1), campus.getBuildingId("MainGate"));
        assertEquals("MainGate", campus.getBuilding(1).getName());
        assertEquals(3, campus.getBuildings().size());
        assertTrue(campus.getBuildingNames().contains("Library"));
    }

    @Test
    public void testIdsToNamesTranslatesPath() {
        Campus campus = sampleCampus();
        List<String> names = campus.idsToNames(Arrays.asList(1, 2, 3));
        assertEquals(Arrays.asList("MainGate", "Library", "Engineering"), names);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsDuplicateBuildingId() {
        List<Building> b = Arrays.asList(
                new Building(1, "A", 0, 0),
                new Building(1, "B", 0, 0));
        Campus.build(b, List.of(), false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsWalkwayWithUnknownBuilding() {
        List<Building> b = List.of(new Building(1, "A", 0, 0));
        List<Walkway>  w = List.of(new Walkway(1, 99, 5.0));
        Campus.build(b, w, false);
    }

    @Test
    public void testGraphIsBuiltCorrectly() {
        Campus campus = sampleCampus();
        assertNotNull(campus.getGraph());
        assertEquals(3, campus.getGraph().getNodes().size());
    }

    private Campus sampleCampus() {
        List<Building> buildings = Arrays.asList(
                new Building(1, "MainGate",    50, 50),
                new Building(2, "Library",    100, 50),
                new Building(3, "Engineering", 150, 50));
        List<Walkway> walkways = Arrays.asList(
                new Walkway(1, 2, 10.0),
                new Walkway(2, 3, 20.0));
        return Campus.build(buildings, walkways, false);
    }
}
