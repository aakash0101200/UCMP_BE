//package com.ucmp.ucmp_backend.model;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Entity
//@Table(name = "admin_details")
//@PrimaryKeyJoinColumn(name = "user_id")
//@Getter
//@Setter
//@NoArgsConstructor
//public class AdminDetail extends User {
//
//    @Enumerated(EnumType.STRING)
//    private AdminLevel adminLevel; // e.g., SUPER_ADMIN, DEPARTMENT_ADMIN
//
//    public AdminDetail(String collegeId, String password, String name, String email, AdminLevel adminLevel) {
//        super(collegeId, password, name, email, Role.ADMIN);
//        this.adminLevel = adminLevel;
//    }
//
//    public enum AdminLevel {
//        SUPER_ADMIN, DEPARTMENT_ADMIN, SUPPORT
//    }
//}
