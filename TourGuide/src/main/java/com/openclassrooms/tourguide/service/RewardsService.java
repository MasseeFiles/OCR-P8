package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.domain.User;
import com.openclassrooms.tourguide.domain.UserReward;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.springframework.stereotype.Service;
import rewardCentral.RewardCentral;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
    private final ExecutorService executor = Executors.newFixedThreadPool(100);
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

    //But de la methode : calcule les recommendations d'attractions à envoyer au user (list userRewards) en fonction de ses visitedLocations et s'il na pas déjà visité l'attraction
    public void calculateRewards(User user) {
        List<VisitedLocation> userVisitedLocations = user.getVisitedLocations();
        List<Attraction> attractions = gpsUtil.getAttractions();
        List<Attraction> reachableAttractions = new ArrayList<>();
        Map<Attraction, VisitedLocation> map = new HashMap<>();
        List<UserReward> userRewards = user.getUserRewards();

        //Etape 1 : filtrage des attractions à proximité + method peek
// flatMap : chaque visitedLocation est representée dans un nouveau stream qui filtre en fonction de la proximité
// en utilisant la list attractions et ajoute l'attraction correspondante dans le nouveau stream
        reachableAttractions.addAll(userVisitedLocations.stream()
                .flatMap(visitedLocation ->
                    attractions.parallelStream()
                            .filter(attraction -> nearAttraction(visitedLocation, attraction))
                            .peek(attraction -> map.put(attraction, visitedLocation))
                )
                .collect(Collectors.toList())
        );

//      //Etape 1 sans Stream
//        for (VisitedLocation visitedLocation : userLocations) {
//            for (Attraction attraction : attractions) {
//                if (nearAttraction(visitedLocation, attraction)) {
//                    reachableAttractions.add(attraction);
//                    map.put(attraction, visitedLocation);
//                }
//            }
//        }

        //Etape 2 : filtrage attractions deja visitées
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            reachableAttractions.parallelStream()
                    .filter(attraction -> userRewards.parallelStream()
                            .anyMatch(reward -> reward.attraction.attractionName.equals(attraction.attractionName)))
                    .forEach(attraction -> map.remove(attraction));
        });

//      //Etape 3 : ajout dans userRewards des attractions filtrées
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

//      //Etape 3 sans stream
//        map.forEach((key, value) -> {
//            Attraction attraction = (Attraction) key;
//            VisitedLocation visitedLocation = (VisitedLocation) value;
//            user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user))); //methode collect()???
//        });
//    }


//// solution avec un stream unique
//        List<UserReward> newUserRewards = new CopyOnWriteArrayList<>();
//
//        CopyOnWriteArrayList<UserReward> userRewards = new CopyOnWriteArrayList<>(user.getUserRewards());
//        CopyOnWriteArrayList<VisitedLocation> visitedLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
//        newUserRewards = visitedLocations.parallelStream().flatMap(visitedLocation ->  //mise en stream de la collection userLocations
//                    attractions.stream()
//                    .filter(attraction -> nearAttraction(visitedLocation, attraction))
//                    .filter(attraction -> userRewards.stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName)))
//                    .map(attraction -> new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)))
//        ).collect(Collectors.toList());
//
//
//        synchronized(user.getUserRewards()) {user.getUserRewards().addAll(newUserRewards);}
//    }



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


