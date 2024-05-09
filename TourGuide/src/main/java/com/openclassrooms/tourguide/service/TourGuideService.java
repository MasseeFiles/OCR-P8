package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.domain.NearbyAttraction;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.domain.User;
import com.openclassrooms.tourguide.domain.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import rewardCentral.RewardCentral;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final RewardCentral rewardsCentral = new RewardCentral();
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocation(user);
		return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().parallelStream().collect(Collectors.toList());		//TODO : modification stream en parallel stream
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
				//TODO : pourquoi lancer cette methode ici?
//
//		user.addToVisitedLocations(visitedLocation);
//		rewardsService.calculateRewards(user);
		return visitedLocation;
	}

	// Modifications pour la recherche des 5 nearbyattractions
		public List<NearbyAttraction> getFiveNearestAttractions(VisitedLocation visitedLocation) {
			List<NearbyAttraction> nearbyAttractionListToReturn = new ArrayList<>();	//valeur de retour
			List<NearbyAttraction> nearbyAttractionListToSort = new ArrayList<>();

			//TODO : possibilité de transformer en Stream???
			for (Attraction attraction : gpsUtil.getAttractions()) {
				Location attractionLocation = new Location(attraction.latitude, attraction.longitude);
				Double attractionDistance = rewardsService.getDistance(visitedLocation.location, attractionLocation);

				NearbyAttraction nearbyAttraction = new NearbyAttraction();
				nearbyAttraction.setNearbyAttractionName(attraction.attractionName);
				nearbyAttraction.setNearbyAttractionLocation(attractionLocation);
				nearbyAttraction.setUserLocation(visitedLocation.location);
				nearbyAttraction.setDistanceToAttraction(attractionDistance);
				nearbyAttraction.setRewardPoints(rewardsCentral.getAttractionRewardPoints(attraction.attractionId, visitedLocation.userId));

				nearbyAttractionListToSort.add(nearbyAttraction);
			}
			/**
			 * Implementation d'un comparateur sur distance attraction-user
			 * dans la classe NearbyAttraction pour implementationn de methode sort
			 * @see NearbyAttraction
			 */
			Collections.sort(nearbyAttractionListToSort);

			if (nearbyAttractionListToSort.size() <= 5) {
				nearbyAttractionListToReturn = nearbyAttractionListToSort;
			} else {
				nearbyAttractionListToReturn = nearbyAttractionListToSort.subList(0, 5);
			}
		return nearbyAttractionListToReturn;
	}
	//Ancienne methode getnearbyattractions remplacée par getfivenearestattractions
//	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
//		List<Attraction> nearbyAttractions = new ArrayList<>();
//		for (Attraction attraction : gpsUtil.getAttractions()) {	//fournit liste de tt attractions
//			if (rewardsService.isWithinAttractionProximity(attraction, visitedLocation.location)) {
//				nearbyAttractions.add(attraction);
//			}
//		}
//		return nearbyAttractions;
//	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
