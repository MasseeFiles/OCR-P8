package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.domain.User;
import com.openclassrooms.tourguide.domain.UserReward;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.junit.jupiter.api.Test;
import rewardCentral.RewardCentral;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRewardsService {

    @Test
    public void userGetRewards_Ok() throws ExecutionException, InterruptedException, TimeoutException {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

        InternalTestHelper.setInternalUserNumber(1);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        Attraction attraction = gpsUtil.getAttractions().get(0);
        user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));  //on met dans la list<visitedLocation> du user la location d'une seule attraction

        //WHEN
        rewardsService.calculateRewards(user);
        tourGuideService.tracker.stopTracking();

//        try {
//            // Time for the completable future in calculateRewards() to complete
//            Thread.sleep(4000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        List<UserReward> userRewardResult = user.getUserRewards();
        assertEquals(1, userRewardResult.size());
    }

    @Test
    public void userGetRewards_No_Rewards() throws ExecutionException, InterruptedException, TimeoutException {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

        InternalTestHelper.setInternalUserNumber(1);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

        //WHEN
        rewardsService.calculateRewards(user);
        tourGuideService.tracker.stopTracking();

//        try {
//            // Time for the completable future in calculateRewards() to complete
//            Thread.sleep(4000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        List<UserReward> userRewardResult = user.getUserRewards();
        assertEquals(0, userRewardResult.size());
    }

    @Test
    public void isWithinAttractionProximity() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        Attraction attraction = gpsUtil.getAttractions().get(0);
        assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
    }

    @Test
    public void nearAllAttractions() throws ExecutionException, InterruptedException, TimeoutException {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        rewardsService.setAttractionProximityRange(Integer.MAX_VALUE);    //valeur max d'un integer - rayon max attraction / userLocation, donc toutes les attractions sont couvertes

        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        Attraction attraction = gpsUtil.getAttractions().get(0);
        user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));

        //WHEN
        rewardsService.calculateRewards(user);
        tourGuideService.tracker.stopTracking();

//        try {
//            // Time for the completable future in calculateRewards() to complete
//            Thread.sleep(40000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        List<UserReward> userRewards = tourGuideService.getUserRewards(user);
        assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
    }
}
