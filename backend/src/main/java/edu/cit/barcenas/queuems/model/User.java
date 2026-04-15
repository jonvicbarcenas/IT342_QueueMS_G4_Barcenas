package edu.cit.barcenas.queuems.model;

public class User {
    private String uid;
    private String email;
    private String password;
    private String firstname;
    private String lastname;
    private String role = "USER"; // default
    private String counterId;
    private String counterName;

    public User() {
    }

    public User(String uid, String email, String password, String firstname, String lastname, String role) {
        this.uid = uid;
        this.email = email;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.role = role;
    }

    public User(String uid, String email, String password, String firstname, String lastname, String role,
            String counterId, String counterName) {
        this(uid, email, password, firstname, lastname, role);
        this.counterId = counterId;
        this.counterName = counterName;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCounterId() {
        return counterId;
    }

    public void setCounterId(String counterId) {
        this.counterId = counterId;
    }

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }
}
