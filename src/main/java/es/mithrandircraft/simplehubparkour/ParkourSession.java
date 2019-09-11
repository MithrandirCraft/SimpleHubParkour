package es.mithrandircraft.simplehubparkour;

import com.google.common.base.Stopwatch;
import org.bukkit.Location;

public class ParkourSession {
    public Stopwatch stopwatch;
    public Location savePoint;
    public int falls = 0;

    ParkourSession(Location firstSavePoint)
    {
        savePoint = firstSavePoint;
        stopwatch = Stopwatch.createStarted();
    }

    public void addFall()
    {
        falls++;
    }
}
