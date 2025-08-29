//package com.ucmp.ucmp_backend.dto;
//
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Pattern;
//import lombok.*;
//
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class CreateScheduleSlotRequest {
//
//    @NotNull
//    private Long sectionId;
//
//    @NotNull
//    private Long facultyId;
//
//    /*
//      Use UPPERCASE DayOfWeek names: MONDAY, TUESDAY, ...
//     */
//    @NotNull
//    private String day;
//
//    // "HH:mm" 24-hr format
//    @NotNull
//    @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "start must be HH:mm")
//    private String start;
//
//    @NotNull
//    @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "end must be HH:mm")
//    private String end;
//
//    @NotNull
//    private String subject;
//
//    private String location;
//}