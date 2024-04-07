package com.openclassrooms.tourguide.service;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.domain.User;
import com.openclassrooms.tourguide.domain.UserReward;

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


	//but pour eviter la concurrentmodification exception :  use a fail-safe iterator.
	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		Iterator<VisitedLocation> iteratorVisitedLocation = userLocations.iterator();

		List<Attraction> attractions = gpsUtil.getAttractions();
		Iterator<Attraction> iteratorAttraction = attractions.iterator();

		List<UserReward> userRewards = user.getUserRewards();
		List<UserReward> copyOfUserRewards = new CopyOnWriteArrayList<>(userRewards);

		while (iteratorVisitedLocation.hasNext()) {
			VisitedLocation visitedLocation = iteratorVisitedLocation.next();
			while (iteratorAttraction.hasNext()) {
				Attraction attraction = iteratorAttraction.next();

				if(copyOfUserRewards.stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
					if(nearAttraction(visitedLocation, attraction)) {
						copyOfUserRewards.add(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));

//		for(VisitedLocation visitedLocation : userLocations) {
//			for(Attraction attraction : attractions) {
//				if(copyOfUserRewards.stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {	//methode d'iteration
//					if(nearAttraction(visitedLocation, attraction)) {
//						copyOfUserRewards.add(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));		//methode de modification
					}
				}
			}
		}
		user.setUserRewards(copyOfUserRewards);

	}
	
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
