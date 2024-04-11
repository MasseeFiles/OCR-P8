package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.domain.User;
import com.openclassrooms.tourguide.domain.UserReward;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.springframework.stereotype.Service;
import rewardCentral.RewardCentral;

import java.util.ArrayList;
import java.util.List;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

    // proximity in miles
    private int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer;
    private int attractionProximityRange = 200;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
    }

    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    //But de la methode : calcule les rewards disponibles en fonction de position du user (getLastVisitedLocation), de proximité des attractions listées (nearAttraction) et du fait que l'attraction a deja été visitée (r.attraction.attractionName.equals(attraction.attractionName)).count() == 0)
    public void calculateRewards(User user) {
        List<VisitedLocation> userLocations = user.getVisitedLocations();
        List<Attraction> attractions = gpsUtil.getAttractions();
        List<Attraction> attractionsNotVisited = new ArrayList<>();
        VisitedLocation currentLocation = user.getLastVisitedLocation();

        //on regarde si user a deja recu un reward de cette attraction - comparaison données du user (userRewards) vs données de l'attraction à verifier (iteration sur les 26 attractions
        for (Attraction attraction : attractions) {
            if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
                attractionsNotVisited.add(attraction);
            }
        }
        //nouvelle boucle pour eviter la Concurrent exception : On regarde dans la boucle si le user est proche de l'attraction à verifier (verification avec currentlocation)
        for (Attraction attraction : attractionsNotVisited) {
            if (nearAttraction(currentLocation, attraction)) {
                user.addUserReward(new UserReward(currentLocation, attraction, getRewardPoints(attraction, user)));
            }
        }
    }
    //Ancienne methode renvoyant la concurrentexception

//			for(VisitedLocation visitedLocation : userLocations) {
//		for(Attraction attraction : attractions) {
//			if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
//				if(nearAttraction(visitedLocation, attraction)) {
//					user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//				}
//			}
//		}
//	}

    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
    }

    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
    }

    private int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
    }

}
