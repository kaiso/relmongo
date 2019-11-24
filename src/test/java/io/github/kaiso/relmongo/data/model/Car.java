package io.github.kaiso.relmongo.data.model;

import io.github.kaiso.relmongo.annotation.ManyToOne;

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

    @Override
    public String toString() {
        return "Car{" +
            "id=" + id +
            ", manufacturer='" + manufacturer + '\'' +
            '}';
    }

}
