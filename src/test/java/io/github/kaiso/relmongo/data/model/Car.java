package io.github.kaiso.relmongo.data.model;

import io.github.kaiso.relmongo.annotation.CascadeType;
import io.github.kaiso.relmongo.annotation.FetchType;
import io.github.kaiso.relmongo.annotation.JoinProperty;
import io.github.kaiso.relmongo.annotation.ManyToOne;
import io.github.kaiso.relmongo.annotation.OneToOne;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Car {

    @Id
    private ObjectId id;
    private String manufacturer;
    private Color color;

    @Indexed(unique = true)
    private int identifier;

    @ManyToOne(mappedBy = "cars")
    private Person owner;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinProperty(name = "location")
    private State location;
    
    public Car(int identifier) {
        super();
        this.identifier = identifier;
    }
    
    public Car() {
        super();
    }
    

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        this.owner = owner;
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public State getLocation() {
        return location;
    }

    public void setLocation(State location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "Car{" +
            "id=" + id +
            ", manufacturer='" + manufacturer + '\'' +
            '}';
    }

}
