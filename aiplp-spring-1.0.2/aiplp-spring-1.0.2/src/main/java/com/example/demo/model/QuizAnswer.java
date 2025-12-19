package com.example.demo.model;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "quiz_answers")
public class QuizAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String question;
    
    @Column(nullable = false)
    private String selectedAnswer;
    
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    @JsonProperty("isCorrect")
    private boolean isCorrect;
    
    @Column(nullable = false)
    private int questionNumber;

    @Column(name = "time_taken", precision = 6, scale = 3)
    private Double timeTaken; // Time taken in seconds with milliseconds

    public QuizAnswer() {
    }

    public QuizAnswer(User user, String question, String selectedAnswer, boolean isCorrect, int questionNumber, Double timeTaken) {
        this.user = user;
        this.question = question;
        this.selectedAnswer = selectedAnswer;
        this.isCorrect = isCorrect;
        this.questionNumber = questionNumber;
        this.timeTaken = timeTaken;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public Double getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Double timeTaken) {
        this.timeTaken = timeTaken;
    }
}
