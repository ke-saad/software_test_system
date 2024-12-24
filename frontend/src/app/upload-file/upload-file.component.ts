import {Component} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {CommonModule} from '@angular/common';
import {MatIconModule} from '@angular/material/icon';

@Component({
  selector: 'app-upload-file',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatProgressBarModule, CommonModule, MatIconModule,],
  templateUrl: './upload-file.component.html',
  styleUrls: ['./upload-file.component.css'],
})
export class UploadFileComponent {
  selectedFile: File | null = null;
  fileContent: string | null = null;
  isAnalyzing = false;
  analysisMessage = 'Analyzing your code...';

  constructor(private http: HttpClient) {
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.readFileContent(this.selectedFile);
    }
  }

  readFileContent(file: File): void {
    const reader = new FileReader();
    reader.onload = () => {
      this.fileContent = reader.result as string;
    };
    reader.onerror = (error) => {
      console.error('Error reading file:', error);
    };
    reader.readAsText(file);
  }

  onUpload(): void {
    if (this.selectedFile) {
      const formData = new FormData();
      formData.append('file', this.selectedFile);

      const apiUrl = 'http://127.0.0.1:8080/api-gateway/CodeAnalysisService';

      this.isAnalyzing = true;
      this.http.post(apiUrl, formData).subscribe({
        next: (response) => {
          console.log('Upload successful:', response);
          this.simulateAnalysis();
        },
        error: (error) => {
          console.error('Upload failed:', error);
          this.isAnalyzing = false;
        },
      });
    } else {
      console.error('No file selected.');
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.selectedFile = event.dataTransfer.files[0];
      this.readFileContent(this.selectedFile);
    }
  }

  triggerFileInput(): void {
    const input = document.querySelector('input[type="file"]') as HTMLElement;
    input.click();
  }

  simulateAnalysis(): void {
    const messages = [
      'Analyzing your code...',
    ];

    let index = 0;
    const interval = setInterval(() => {
      this.analysisMessage = messages[index];
      index++;
      if (index >= messages.length) {
        clearInterval(interval);
        this.isAnalyzing = false;
        this.analysisMessage = 'Analysis complete!';
      }
    }, 2000);
  }
}
