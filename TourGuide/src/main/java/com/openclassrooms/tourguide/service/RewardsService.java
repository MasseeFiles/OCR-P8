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
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

    //But de la methode : calcule les recommendations à envoyer au user (list userrewards dans l'objet user) en fonction de ses visitedlocations
    public void calculateRewards(User user) {
        List<VisitedLocation> userLocations = user.getVisitedLocations();
        List<Attraction> attractions = gpsUtil.getAttractions();
        List<Attraction> attractionsNotVisited = new ArrayList<>();
        List<Attraction> reachableAttractions = new ArrayList<>();
        Map<Attraction, VisitedLocation> mapToSort = new HashMap<>();
        List<UserReward> userRewards = user.getUserRewards();

        //on recupere les attractions à portée de chaque VisitedLocation du User (nearAttraction) et on les met dans une map et dans une liste
        //poss de combiner les 2 boucles visited et attractions - fin de projet
        for (VisitedLocation visitedLocation : userLocations) {
            attractions.parallelStream().forEach(attraction -> {
                if (nearAttraction(visitedLocation, attraction)) {
                    reachableAttractions.add(attraction);
                    mapToSort.put(attraction, visitedLocation);
                }
            } );
//          for (VisitedLocation visitedLocation : userLocations) {
//              for (Attraction attraction : attractions) {
//                if (nearAttraction(visitedLocation, attraction)) {
//                    reachableAttractions.add(attraction);
//                    mapToSort.put(attraction, visitedLocation);
//                }
//            }
        }
        //on enleve de la map obtenue  les attractions déjà visitées
        reachableAttractions.parallelStream().forEach(attraction -> {
            if (userRewards.stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() > 0) {
                mapToSort.remove(attraction);
            }
        } );
//        for (Attraction attraction : reachableAttractions) {
//            if (userRewards.stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() > 0) { //attraction visitée
//                mapToSort.remove(attraction);
//            }
//        }

        //on met à jour les userrewards du user avec les données de la map precedemment filtrée
        //ajouter ici completablefuture
        mapToSort.forEach((key, value) -> {
            Attraction attraction = (Attraction) key;
            VisitedLocation visitedLocation = (VisitedLocation) value;
            user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user))); //partie a isoler pour concurent exception
        });
//        Stream<Map.Entry<Attraction, VisitedLocation>> streamToSort = mapToSort.entrySet().parallelStream();
//        streamToSort.forEach((key, value) -> {
////            Attraction attraction = (Attraction) key;
////            VisitedLocation visitedLocation = (VisitedLocation) value;
//            user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user))); //partie a isoler pour concurent exception
//        });

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


//          //Autre solution
    //        //on regarde si user a deja recu un reward de cette attraction - comparaison données du user (userRewards) vs données de l'attraction à verifier (iteration sur les 26 attractions
//        for (Attraction attraction : attractions) {
//            if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
//                attractionsNotVisited.add(attraction);
//            }
//        }
//        //nouvelle boucle pour eviter la Concurrent exception : On regarde dans la boucle si le user est proche de l'attraction à verifier (verification avec currentlocation)
//        for (Attraction attraction : attractionsNotVisited) {
//            if (nearAttraction(currentLocation, attraction)) {
//                user.addUserReward(new UserReward(currentLocation, attraction, getRewardPoints(attraction, user)));
//            }
//        }
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
