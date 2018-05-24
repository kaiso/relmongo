package io.github.kaiso.relmongo.data.model;

import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.OneToMany;
import io.github.kaiso.relmongo.annotation.OneToOne;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "people")
public class Person {
    private ObjectId id;
    private String name;
    private String email;
    
    @OneToMany(fetch=FetchType.EAGER)
    @JoinProperty(name="cars_ids")
    private List<Car> cars;
    
    @OneToMany(fetch=FetchType.LAZY)
    @JoinProperty(name="houses_ids")
    private List<House> houses;
    
    @OneToOne(fetch=FetchType.EAGER)
    @JoinProperty(name = "passportId")
    private Passport passport;
    
    @OneToOne(fetch=FetchType.LAZY)
    @JoinProperty(name = "drivingLicenseId")
    private DrivingLicense drivingLicense;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public List<Car> getCars() {
        return cars;
    }
    public void setCars(List<Car> cars) {
        this.cars = cars;
    }
    public ObjectId getId() {
        return id;
    }
    public void setId(ObjectId id) {
        this.id = id;
    }
    public List<House> getHouses() {
        return houses;
    }
    public void setHouses(List<House> houses) {
        this.houses = houses;
    }
    public Passport getPassport() {
        return passport;
    }
    public void setPassport(Passport passport) {
        this.passport = passport;
    }
    public DrivingLicense getDrivingLicense() {
        return drivingLicense;
    }
    public void setDrivingLicense(DrivingLicense drivingLicense) {
        this.drivingLicense = drivingLicense;
    }
    
    

    
    
}
