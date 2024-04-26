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

    //But de la methode : calcule les recommendations à envoyer au user (list userRewards dans l'objet user) en fonction de ses visitedLocations
    public void calculateRewards(User user) {
        List<VisitedLocation> userLocations = user.getVisitedLocations();
        List<Attraction> attractions = gpsUtil.getAttractions();
        List<Attraction> reachableAttractions = new ArrayList<>();
        Map<Attraction, VisitedLocation> map = new HashMap<>();   //utilisation de concurenthashmap???
        List<UserReward> userRewards = user.getUserRewards();
//        List<Attraction> attractions = new CopyOnWriteArrayList<>(gpsUtil.getAttractions());

        //    //solution avec stream - method peek
                    //etape 1 : filtrage proximité
        reachableAttractions.addAll(userLocations.parallelStream()
                .flatMap(visitedLocation ->
                        attractions.parallelStream()
                                .filter(attraction -> nearAttraction(visitedLocation, attraction))
                                .peek(attraction -> map.put(attraction, visitedLocation))
                )
                .collect(Collectors.toList()));

                    //etape 2 : filtrage attractions deja visitées
//        reachableAttractions.parallelStream()
//                .filter(attraction -> userRewards.parallelStream()
//                        .anyMatch(r -> r.attraction.attractionName.equals(attraction.attractionName)))
//                .forEach(attraction -> map.remove(attraction));

        //version avec completable future
        CompletableFuture<Void> futureRemoval = CompletableFuture.runAsync(() -> {
            reachableAttractions.parallelStream()
                    .filter(attraction -> userRewards.parallelStream()
                            .anyMatch(r -> r.attraction.attractionName.equals(attraction.attractionName)))
                    .forEach(attraction -> map.remove(attraction));
        });


                    //etape 3 : ajout des recommandations dans le userRewards du user
        //solution avec stream
//        map.entrySet().parallelStream()
//                .map(entry -> {
//                    Attraction attraction = (Attraction) entry.getKey();
//                    VisitedLocation visitedLocation = (VisitedLocation) entry.getValue();
//                    int rewardPoints = getRewardPoints(attraction, user);
//                    return new UserReward(visitedLocation, attraction, rewardPoints);
//                })
//                .forEach(user::addUserReward);
//    }
                    //version avec utilisation du completable future
        futureRemoval.thenRun(() -> {
            map.entrySet().parallelStream()
                    .map(entry -> {
                        Attraction attraction = (Attraction) entry.getKey();
                        VisitedLocation visitedLocation = (VisitedLocation) entry.getValue();
                        int rewardPoints = getRewardPoints(attraction, user);
                        return new UserReward(visitedLocation, attraction, rewardPoints);
                    })
                    .forEach(user::addUserReward);
        });
            }

//       //ETAPE 2 : on enleve de la map obtenue les attractions déjà visitées

        //solution avec parallel stream
//        reachableAttractions.forEach(attraction -> {
//            if (userRewards.parallelStream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() > 0) {
//                map.remove(attraction);
//            }
//        });

//        //ETAPE 3 : on met à jour les userRewards du user avec les données de la map precedemment filtrée

//        //solution sans stream
//        map.forEach((key, value) -> {
//            Attraction attraction = (Attraction) key;
//            VisitedLocation visitedLocation = (VisitedLocation) value;
//            user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user))); //methode collect()???
//        });
//    }



        //poss de combiner les 2 boucles visited et attractions - fin de projet


        //Enchainement de completable future - pas possible
//        // etape1
//        List<VisitedLocation> visitedLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
//        List<UserReward> userRewardsToAdd = new CopyOnWriteArrayList<>();
//        List<UserReward> userRewardsToAddStep1 = new CopyOnWriteArrayList<>();
//        List<UserReward> userRewardsToAddStep2 = new CopyOnWriteArrayList<>();
//        List<Attraction> attractions = new CopyOnWriteArrayList<>(gpsUtil.getAttractions());
//
//        CompletableFuture<List<UserReward>> step1 = new CompletableFuture<>();  //filtrage proximité
//        Executors.newCachedThreadPool().submit(() -> {
//            List<UserReward> listStep1 = visitedLocations.parallelStream()
//                    .flatMap(visitedLocation ->
//                            attractions.stream()
//                                    .filter(attraction -> nearAttraction(visitedLocation, attraction))
//                                    .map(attraction -> new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)))
//                    )
//                    .collect(Collectors.toList()); // accumulation des elements dans une liste
//            step1.complete(listStep1);
//
//            return null;
////            return step1;
//        });


//etape 2

//        CompletableFuture<Map> futureResult = new CompletableFuture<>();        //futureResult représente le resultat de l'étape 2
//
//        Executors.newCachedThreadPool().submit(() -> {
////            reachableAttractions.forEach(attraction -> {
//            reachableAttractions.parallelStream().forEach(attraction -> {
//                if (userRewards.stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() > 0) {
//                    map.remove(attraction);
//                }
//            });
//            return futureResult;
//        });


////etape 1
//        userRewardsToAddStep1 = visitedLocations.parallelStream().flatMap(visitedLocation ->
//                attractions.stream()
//                        .filter(attraction -> nearAttraction(visitedLocation, attraction))
//                        .map(attraction -> new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)))
//        ).collect(Collectors.toList());

        //Etape2
//        List<Attraction> attractions = new CopyOnWriteArrayList<>(gpsUtil.getAttractions());

//        userRewardsToAddStep2 = userRewardsToAddStep1.parallelStream()
//                .flatMap(userReward ->
//                        attractions.stream()
////                            .filter(userReward -> attractions.noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName)))
//                            .filter(userReward -> attractions.stream().anyMatch(attraction -> userReward.attraction.attractionName.equals(attraction.attractionName))  // si equals, attraction deja visitée
////                        .map(attraction -> new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)))
//        ).collect(Collectors.toList());
//
//            user.getUserRewards().addAll(userRewardsToAddStep2);

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


//        //solution sans stream
//        for (VisitedLocation visitedLocation : userLocations) {
//            for (Attraction attraction : attractions) {
//                if (nearAttraction(visitedLocation, attraction)) {
//                    reachableAttractions.add(attraction);
//                    map.put(attraction, visitedLocation);
//                }
//            }
//        }





//        //Solution avec COMPLETABLE FUTURE : on lie les ETAPES 2 et 3

        //CompletableFuture - methode 1 - try/catch
        //futureResult représente le resultat de l'étape 2

//        CompletableFuture<Map> futureResult = CompletableFuture.supplyAsync(() -> {   //bizarre la methode supplyASync ne declenche pas seule l'execution du completable
//            try {   //supplier
//                reachableAttractions.forEach(attraction -> {
//                    if (userRewards.stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() > 0) {
//                        map.remove(attraction);
//                    }
//                });
//            } catch (Exception e) {  //exception pour savoir si thread s'arrete - A VOIR TYPE D'EXCEPTION PLUS PERTINENT
//                throw new RuntimeException(e);
//            }
//            return map;
//        });

//        //  //CompletableFuture - methode 2 - executor
//        CompletableFuture<Map> futureResult = new CompletableFuture<>();        //futureResult représente le resultat de l'étape 2
//
//        Executors.newCachedThreadPool().submit(() -> {
////            reachableAttractions.forEach(attraction -> {
//            reachableAttractions.parallelStream().forEach(attraction -> {
//                if (userRewards.stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() > 0) {
//                    map.remove(attraction);
//                }
//            });
//            return futureResult;
//        });

//        futureResult.complete(map);    //declenchement de l'execution du completable sans la methode Async()- execution asunchrone quand meme???
//
//        futureResult.thenAccept((mapFiltered) -> {      //dès que future est pret, on declenche l'étape finale (utilisation de methode thenAccept() + addUserRewardsFromMap() qui réalise l'etape 3
//            addUserRewardsFromMap(mapFiltered, user);
//        });

//    }



        //autre : appel a la method sortMap() implémentée plus bas
        //        CompletableFuture<Map> futureResult = sortMap(reachableAttractions, user.getUserRewards(), map);

    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
    }

    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
    }

    private int getRewardPoints(Attraction attraction, User user) {
        Future<Integer> future = executor.submit(() -> {return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
        });
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
//        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
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

//    private CompletableFuture<Map> sortMap(List<Attraction> reachableAttractions, List<UserReward> userRewards, Map<Attraction, VisitedLocation> map) {
//        //supplyAsync() permet d'executer la tache definie dans la lambda de facon asyncrone
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                reachableAttractions.forEach(attraction -> {
//                    if (userRewards.stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() > 0) {
//                        map.remove(attraction);
//                    }
//                });
//            } catch (Exception e) {  //exception pour savoir si thread s'arrete
//                throw new RuntimeException(e);
//            }
//            return map;
//        });
//    }
//    private void addUserRewardsFromMap (Map<Attraction, VisitedLocation> mapFiltered, User user) {
//        mapFiltered.forEach((key, value) -> {
//            Attraction attraction = (Attraction) key;
//            VisitedLocation visitedLocation = (VisitedLocation) value;
//            try {
//                user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//            } catch (ExecutionException e) {
//                throw new RuntimeException(e);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }
}


