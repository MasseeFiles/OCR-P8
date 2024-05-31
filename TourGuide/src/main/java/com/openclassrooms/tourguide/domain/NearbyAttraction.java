package com.openclassrooms.tourguide.domain;

import gpsUtil.location.Location;

public class NearbyAttraction implements Comparable<NearbyAttraction> {
    private String nearbyAttractionName;
    private Location nearbyAttractionLocation;
    private Location userLocation;
    private Double distanceToAttraction;
    private int rewardPoints;

    public NearbyAttraction(String nearbyAttractionName, Location nearbyAttractionLocation, Location userLocation, Double distanceToAttraction, int rewardPoints) {
        this.nearbyAttractionName = nearbyAttractionName;
        this.nearbyAttractionLocation = nearbyAttractionLocation;
        this.userLocation = userLocation;
        this.distanceToAttraction = distanceToAttraction;
        this.rewardPoints = rewardPoints;
    }

    public NearbyAttraction() {
    }

    public String getNearbyAttractionName() {
        return nearbyAttractionName;
    }

    public void setNearbyAttractionName(String nearbyAttractionName) {
        this.nearbyAttractionName = nearbyAttractionName;
    }

    public Location getNearbyAttractionLocation() {
        return nearbyAttractionLocation;
    }

    public void setNearbyAttractionLocation(Location nearbyAttractionLocation) {
        this.nearbyAttractionLocation = nearbyAttractionLocation;
    }

    public Location getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
    }

    public Double getDistanceToAttraction() {
        return distanceToAttraction;
    }

    public void setDistanceToAttraction(Double distanceToAttraction) {
        this.distanceToAttraction = distanceToAttraction;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    /**
     * Implementation de la methode compareTo() utilisé par  la
     * methode sort() dans la methode getFiveNearestAttractions()
     *
     * @see com.openclassrooms.tourguide.service.TourGuideService
     */
    @Override
    public int compareTo(NearbyAttraction otherNearbyAttraction) {
        return Double.compare(this.distanceToAttraction, otherNearbyAttraction.distanceToAttraction);
    }
}
