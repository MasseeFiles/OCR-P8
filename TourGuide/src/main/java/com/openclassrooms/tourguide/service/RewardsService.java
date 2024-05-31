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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
    private final ExecutorService executor = Executors.newFixedThreadPool(100);
    private int defaultAttractionProximityRange = 10;
    private int attractionProximityRange = defaultAttractionProximityRange;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
    }

    public void setAttractionProximityRange(int attractionProximityRange) {
        this.attractionProximityRange = attractionProximityRange;
    }

    /*But de la calculateRewards() : définit les recommendations d'attractions à ajouter dans les attributs
     d'un user (List<UserRewards>) en fonction de la proximité avec ses visitedLocations (étape 1) et s'il n'a pas
     déjà visité les attraction (étape2)
     */
    public void calculateRewards(User user) {
        List<VisitedLocation> userVisitedLocations = user.getVisitedLocations();
        List<Attraction> attractions = gpsUtil.getAttractions();
        List<Attraction> attractionsInRange = new ArrayList<>();
        Map<Attraction, VisitedLocation> map = new HashMap<>();
        List<UserReward> userRewards = user.getUserRewards();

        //Etape 1 : filtrage des attractions suffisamment proches
        filterAttractionsInRange(userVisitedLocations, attractions, attractionsInRange, map);

        //Etape 2 : filtrage attractions deja visitées
        CompletableFuture<Void> future = getVoidCompletableFuture(attractionsInRange, map, userRewards);

        //Etape 3 : ajout dans userRewards des attractions filtrées
        addRewardsFromFuture(user, map, future);
    }

    private void filterAttractionsInRange
            (List<VisitedLocation> userVisitedLocations,
             List<Attraction> attractions,
             List<Attraction> attractionsInRange,
             Map<Attraction, VisitedLocation> map) {
        attractionsInRange.addAll(userVisitedLocations.stream()
                .flatMap(visitedLocation ->
                        attractions.parallelStream()
                                .filter(attraction -> isWithinAttractionProximity(attraction, visitedLocation.location))
                                .peek(attraction -> map.put(attraction, visitedLocation))
                )
                .collect(Collectors.toList())
        );
    }

    private static CompletableFuture<Void> getVoidCompletableFuture(List<Attraction> attractionsInRange, Map<Attraction, VisitedLocation> map, List<UserReward> userRewards) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            attractionsInRange.parallelStream()
                    .filter(attraction -> userRewards.parallelStream()
                            .anyMatch(reward -> reward.attraction.attractionName.equals(attraction.attractionName)))
                    .forEach(attraction -> map.remove(attraction));
        });
        return future;
    }

    private void addRewardsFromFuture(User user, Map<Attraction, VisitedLocation> map, CompletableFuture<Void> future) {
        future.thenRun(() -> {
            map.entrySet().parallelStream()
                    .forEach(entry -> {
                        Attraction attraction = (Attraction) entry.getKey();
                        VisitedLocation visitedLocation = (VisitedLocation) entry.getValue();
                        int rewardPoints = getRewardPoints(attraction, user);
                        user.addUserReward(new UserReward(visitedLocation, attraction, rewardPoints));
                    });
        });
    }

    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
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


