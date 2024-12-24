package com.CodeAnalysis.service.model;
import jakarta.persistence.*;
import lombok.*;
@AllArgsConstructor
@Entity
@Data
public class AnalysisResult {
    public AnalysisResult() {
		super();
	}

	public AnalysisResult(String fileName, String analysisDetails, String status) {
		super();
		this.fileName = fileName;
		this.analysisDetails = analysisDetails;
		this.status = status;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String analysisDetails;
    
	@Column(nullable = false)
    private String status;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getAnalysisDetails() {
		return analysisDetails;
	}

	public void setAnalysisDetails(String analysisDetails) {
		this.analysisDetails = analysisDetails;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}


}