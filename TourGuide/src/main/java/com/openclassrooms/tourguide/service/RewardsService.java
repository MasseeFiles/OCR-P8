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
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
    CopyOnWriteArrayList<Attraction> attractions;

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
        this.attractions = new CopyOnWriteArrayList<>(gpsUtil.getAttractions());
    }

    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    //But de la methode : calcule les recommendations à envoyer au user (list userRewards dans l'objet user) en fonction de ses visitedLocations
    public void calculateRewards(User user) {
//        List<VisitedLocation> userLocations = user.getVisitedLocations();
//        List<Attraction> attractions = gpsUtil.getAttractions();
//        List<Attraction> reachableAttractions = new ArrayList<>();
//        Map<Attraction, VisitedLocation> map = new HashMap<>();   //utilisation de concurenthashmap???
//        List<UserReward> userRewards = user.getUserRewards();


        //poss de combiner les 2 boucles visited et attractions - fin de projet

        //ETAPE 1  : on recupere les attractions à portée de chaque VisitedLocation du User (nearAttraction) et on les met dans une map et dans une liste

        //       //solution avec stream
        List<UserReward> newUserRewards = new CopyOnWriteArrayList<>();
//        user.getVisitedLocations().stream().map(visitedLocation -> { //mise en stream de la collection userLocations
//                    attractions.stream().map(attraction -> {
//                        if (nearAttraction(visitedLocation, attraction)) {
//                            System.out.println(attraction.attractionName);
//                            System.out.println(user.getUserRewards());
//
//                            if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0 ) {
////                                reachableAttractions.add(attraction);
//                                try {
//                                    newUserRewards.add(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//                                } catch (ExecutionException e) {
//                                    throw new RuntimeException(e);
//                                } catch (InterruptedException e) {
//                                    throw new RuntimeException(e);
//                                }
//                            }
//                        }
//return 0;
//                    }).count();
//            return 0;
//                }).count();





        CopyOnWriteArrayList<UserReward> userRewards = new CopyOnWriteArrayList<>(user.getUserRewards());
        CopyOnWriteArrayList<VisitedLocation> visitedLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
        newUserRewards = visitedLocations.parallelStream().flatMap(visitedLocation ->  //mise en stream de la collection userLocations
            attractions.stream()
                    .filter(attraction -> nearAttraction(visitedLocation, attraction))
                    .filter(attraction -> userRewards.stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName)))
                    .map(attraction -> new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)))
        ).collect(Collectors.toList());


        synchronized(user.getUserRewards()) {user.getUserRewards().addAll(newUserRewards);}

//                        if () {
//                            System.out.println(attraction.attractionName);
//                            System.out.println(user.getUserRewards());
//
//                            if  {
////                                reachableAttractions.add(attraction);
//                                try {
//                                    newUserRewards.add(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//                                } catch (ExecutionException e) {
//                                    throw new RuntimeException(e);
//                                } catch (InterruptedException e) {
//                                    throw new RuntimeException(e);
//                                }
//                            }
//                        }
//return 0;
//                    }).count();
//            return 0;
//                }).count();

//                    user.getUserRewards().add(new UserReward(null, null));

    }
        //solution sans stream
//        for (VisitedLocation visitedLocation : userLocations) {
//            for (Attraction attraction : attractions) {
//                if (nearAttraction(visitedLocation, attraction)) {
//                    reachableAttractions.add(attraction);
//                    map.put(attraction, visitedLocation);
//                }
//            }
//        }

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

        //          //solution avec stream
//        Stream<Map.Entry<Attraction, VisitedLocation>> stream = map.entrySet().stream();        //map transformée en set entryset(), set transformé en stream donc collection stream()
//        stream.parallel().forEach(attractionVisitedLocationEntry -> { //transformation en stream parallel
//            Attraction attraction = attractionVisitedLocationEntry.getKey();
//            VisitedLocation visitedLocation = attractionVisitedLocationEntry.getValue();
//            user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//        });
//    }

//        //Solution avec Completable Future : on lie les ETAPES 2 et 3

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
//        return true;
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


