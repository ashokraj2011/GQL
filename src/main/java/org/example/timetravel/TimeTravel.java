package org.example.timetravel;

import org.example.data.DataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for handling time-travel queries, allowing data to be queried 
 * as it existed at a specific point in time.
 */
@Service
public class TimeTravel {
    
    private final DataLoader dataLoader;

    @Autowired
    public TimeTravel(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }
    
    /**
     * Set the timestamp for time-travel queries
     * 
     * @param timestamp The point in time to query data from
     */
    public void setTimestamp(Instant timestamp) {
        dataLoader.setTimestamp(timestamp);
        System.out.println("TimeTravel: Set timestamp to " + timestamp);
    }
    
    /**
     * Reset the timestamp to return to current data
     */
    public void resetTimestamp() {
        dataLoader.resetTimestamp();
        System.out.println("TimeTravel: Reset timestamp to current");
    }
    
    /**
     * Get the current time-travel timestamp, if set
     * 
     * @return The current timestamp, or null if not in time-travel mode
     */
    public Instant getCurrentTimestamp() {
        // This would need to be implemented in DataLoader to expose the current timestamp
        // For now, we just return null
        return null;
    }
    
    /**
     * Check if time-travel mode is active
     * 
     * @return true if in time-travel mode, false otherwise
     */
    public boolean isTimeTravelActive() {
        return getCurrentTimestamp() != null;
    }
}
