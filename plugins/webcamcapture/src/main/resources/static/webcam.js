/**
 * WebCamCapture Plugin - Client-side script
 * This script handles webcam/microphone access and media capture in the browser
 */
class WebCamCapture {
    constructor(options = {}) {
        this.options = Object.assign({
            photoButton: '#takePhotoButton',
            videoButton: '#recordVideoButton',
            stopButton: '#stopRecordingButton',
            videoElement: '#videoPreview',
            canvasElement: '#photoCanvas',
            photoPreview: '#photoPreview',
            errorMessage: '#errorMessage',
            apiEndpoint: '/api/debug/WebCamCapture/process',
            videoMimeType: 'video/webm',
            videoBitsPerSecond: 2500000, // 2.5 Mbps
            videoConstraints: {
                width: { ideal: 1280 },
                height: { ideal: 720 },
                facingMode: 'user' // or 'environment' for back camera
            },
            audioConstraints: {
                echoCancellation: true,
                noiseSuppression: true,
                autoGainControl: true
            }
        }, options);

        this.stream = null;
        this.mediaRecorder = null;
        this.recordedChunks = [];
        this.recording = false;

        this.videoElement = document.querySelector(this.options.videoElement);
        this.canvasElement = document.querySelector(this.options.canvasElement);
        this.photoPreview = document.querySelector(this.options.photoPreview);
        this.errorMessage = document.querySelector(this.options.errorMessage);

        // Initialize elements if they don't exist
        this._initializeElements();

        // Set up event listeners
        this._setupEventListeners();
    }

    /**
     * Initialize required HTML elements if they don't exist
     */
    _initializeElements() {
        const container = document.createElement('div');
        container.id = 'webcam-capture-container';
        container.className = 'webcam-capture-container';

        // Only append if the container doesn't already exist
        if (!document.getElementById('webcam-capture-container')) {
            // Create HTML structure if elements don't exist
            let html = `
                <div class="webcam-preview-container">
                    <video id="videoPreview" autoplay playsinline></video>
                    <canvas id="photoCanvas" style="display:none;"></canvas>
                    <div class="media-preview">
                        <img id="photoPreview" alt="Captured photo" style="display:none;">
                    </div>
                </div>
                <div class="webcam-controls">
                    <button id="takePhotoButton" class="webcam-button">Take Photo</button>
                    <button id="recordVideoButton" class="webcam-button">Record Video</button>
                    <button id="stopRecordingButton" class="webcam-button" style="display:none;">Stop Recording</button>
                </div>
                <div id="errorMessage" class="error-message"></div>
                <div class="recording-indicator"></div>
                <div class="status-message"></div>
            `;

            container.innerHTML = html;
            document.body.appendChild(container);

            // Update element references
            this.videoElement = document.querySelector('#videoPreview');
            this.canvasElement = document.querySelector('#photoCanvas');
            this.photoPreview = document.querySelector('#photoPreview');
            this.errorMessage = document.querySelector('#errorMessage');
        }

        // Apply basic styles
        if (!document.getElementById('webcam-capture-styles')) {
            const style = document.createElement('style');
            style.id = 'webcam-capture-styles';
            style.textContent = `
                .webcam-capture-container {
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    font-family: Arial, sans-serif;
                }
                .webcam-preview-container {
                    position: relative;
                    width: 100%;
                    margin-bottom: 20px;
                    border-radius: 8px;
                    overflow: hidden;
                    background-color: #f0f0f0;
                }
                #videoPreview {
                    width: 100%;
                    max-height: 480px;
                    background-color: #000;
                }
                .media-preview {
                    width: 100%;
                    text-align: center;
                    padding: 10px 0;
                }
                #photoPreview {
                    max-width: 100%;
                    max-height: 480px;
                    border-radius: 4px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                }
                .webcam-controls {
                    display: flex;
                    justify-content: center;
                    gap: 10px;
                    margin-bottom: 15px;
                }
                .webcam-button {
                    padding: 10px 15px;
                    background-color: #4285f4;
                    color: white;
                    border: none;
                    border-radius: 4px;
                    cursor: pointer;
                    font-size: 14px;
                }
                .webcam-button:hover {
                    background-color: #3367d6;
                }
                #stopRecordingButton {
                    background-color: #ea4335;
                }
                #stopRecordingButton:hover {
                    background-color: #d33828;
                }
                .error-message {
                    color: #ea4335;
                    font-size: 14px;
                    margin-top: 10px;
                    text-align: center;
                }
                .recording-indicator {
                    width: 15px;
                    height: 15px;
                    border-radius: 50%;
                    background-color: #ea4335;
                    position: absolute;
                    top: 15px;
                    right: 15px;
                    display: none;
                }
                .recording-indicator.active {
                    display: block;
                    animation: blink 1s infinite;
                }
                @keyframes blink {
                    0% { opacity: 1; }
                    50% { opacity: 0; }
                    100% { opacity: 1; }
                }
                .status-message {
                    text-align: center;
                    font-size: 14px;
                    color: #666;
                    margin-top: 10px;
                }
            `;
            document.head.appendChild(style);
        }
    }

    /**
     * Set up event listeners for control buttons
     */
    _setupEventListeners() {
        const takePhotoButton = document.querySelector(this.options.photoButton);
        const recordVideoButton = document.querySelector(this.options.videoButton);
        const stopButton = document.querySelector(this.options.stopButton);

        if (takePhotoButton) {
            takePhotoButton.addEventListener('click', () => this.takePhoto());
        }

        if (recordVideoButton) {
            recordVideoButton.addEventListener('click', () => this.toggleRecording());
        }

        if (stopButton) {
            stopButton.addEventListener('click', () => this.stopRecording());
        }
    }

    /**
     * Initialize the webcam stream
     * @returns {Promise} A promise that resolves when the webcam is initialized
     */
    async initialize() {
        try {
            // Request camera and microphone permissions
            const constraints = {
                video: this.options.videoConstraints,
                audio: this.options.audioConstraints
            };

            this.stream = await navigator.mediaDevices.getUserMedia(constraints);

            // Connect the stream to the video element
            this.videoElement.srcObject = this.stream;

            // Set up the canvas with the correct dimensions
            this.videoElement.addEventListener('loadedmetadata', () => {
                this.canvasElement.width = this.videoElement.videoWidth;
                this.canvasElement.height = this.videoElement.videoHeight;
            });

            this._showStatus('Camera initialized successfully');
            this._hideError();
            return true;

        } catch (error) {
            this._showError('Error accessing camera: ' + error.message);
            console.error('Error initializing webcam:', error);
            return false;
        }
    }

    /**
     * Take a photo from the webcam
     */
    takePhoto() {
        if (!this.stream) {
            this._showError('Camera is not initialized');
            return;
        }

        try {
            // Draw the current video frame to the canvas
            const context = this.canvasElement.getContext('2d');
            context.drawImage(this.videoElement, 0, 0, this.canvasElement.width, this.canvasElement.height);

            // Convert to data URL (JPEG format)
            const imageData = this.canvasElement.toDataURL('image/jpeg', 0.9);

            // Display the captured image
            this.photoPreview.src = imageData;
            this.photoPreview.style.display = 'block';

            // Upload the photo to the server
            this._uploadPhoto(imageData);

            this._showStatus('Photo captured!');

        } catch (error) {
            this._showError('Error capturing photo: ' + error.message);
            console.error('Error taking photo:', error);
        }
    }

    /**
     * Toggle video recording on/off
     */
    toggleRecording() {
        if (this.recording) {
            this.stopRecording();
        } else {
            this.startRecording();
        }
    }

    /**
     * Start recording video
     */
    startRecording() {
        if (!this.stream) {
            this._showError('Camera is not initialized');
            return;
        }

        try {
            // Reset recorded chunks
            this.recordedChunks = [];

            // Create a MediaRecorder with the stream
            const options = {
                mimeType: this._getSupportedMimeType(),
                videoBitsPerSecond: this.options.videoBitsPerSecond
            };

            this.mediaRecorder = new MediaRecorder(this.stream, options);

            // Add event listeners
            this.mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    this.recordedChunks.push(event.data);
                }
            };

            this.mediaRecorder.onstop = () => {
                this._processRecordedVideo();
            };

            // Start recording
            this.mediaRecorder.start(1000); // Collect data every second
            this.recording = true;

            // Update UI
            document.querySelector(this.options.videoButton).style.display = 'none';
            document.querySelector(this.options.stopButton).style.display = 'block';
            document.querySelector('.recording-indicator').classList.add('active');

            this._showStatus('Recording video...');

        } catch (error) {
            this._showError('Error starting recording: ' + error.message);
            console.error('Error starting recording:', error);
        }
    }

    /**
     * Stop recording video
     */
    stopRecording() {
        if (!this.mediaRecorder || this.mediaRecorder.state === 'inactive') {
            this._showError('No active recording found');
            return;
        }

        try {
            // Stop the media recorder
            this.mediaRecorder.stop();
            this.recording = false;

            // Update UI
            document.querySelector(this.options.videoButton).style.display = 'block';
            document.querySelector(this.options.stopButton).style.display = 'none';
            document.querySelector('.recording-indicator').classList.remove('active');

            this._showStatus('Recording stopped. Processing video...');

        } catch (error) {
            this._showError('Error stopping recording: ' + error.message);
            console.error('Error stopping recording:', error);
        }
    }

    /**
     * Process the recorded video chunks and upload to server
     */
    _processRecordedVideo() {
        try {
            if (this.recordedChunks.length === 0) {
                this._showError('No video data recorded');
                return;
            }

            // Create a Blob from the recorded chunks
            const mimeType = this._getSupportedMimeType();
            const videoBlob = new Blob(this.recordedChunks, { type: mimeType });

            // Create a URL for the blob
            const videoURL = URL.createObjectURL(videoBlob);

            // Update the video element to play the recording
            this.videoElement.srcObject = null;
            this.videoElement.src = videoURL;
            this.videoElement.controls = true;

            // Upload the video to the server
            this._uploadVideo(videoBlob, mimeType);

            this._showStatus('Video processed successfully!');

        } catch (error) {
            this._showError('Error processing video: ' + error.message);
            console.error('Error processing video:', error);
        }
    }

    /**
     * Upload a photo to the server
     * @param {string} imageData - Base64 encoded image data
     */
    async _uploadPhoto(imageData) {
        try {
            const response = await fetch(this.options.apiEndpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    operation: 'takePhoto',
                    imageData: imageData
                })
            });

            const data = await response.json();

            if (data.error) {
                this._showError('Server error: ' + data.error);
                return;
            }

            this._showStatus(`Photo saved as ${data.fileName}`);

        } catch (error) {
            this._showError('Error uploading photo: ' + error.message);
            console.error('Error uploading photo:', error);
        }
    }

    /**
     * Upload a video to the server
     * @param {Blob} videoBlob - Video data as Blob
     * @param {string} mimeType - MIME type of the video
     */
    async _uploadVideo(videoBlob, mimeType) {
        try {
            // Convert Blob to Base64
            const reader = new FileReader();

            reader.onloadend = async () => {
                const base64data = reader.result;

                const response = await fetch(this.options.apiEndpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        operation: 'recordVideo',
                        videoData: base64data,
                        mimeType: mimeType
                    })
                });

                const data = await response.json();

                if (data.error) {
                    this._showError('Server error: ' + data.error);
                    return;
                }

                this._showStatus(`Video saved as ${data.fileName}`);
            };

            reader.readAsDataURL(videoBlob);

        } catch (error) {
            this._showError('Error uploading video: ' + error.message);
            console.error('Error uploading video:', error);
        }
    }

    /**
     * Get the first supported MIME type for video recording
     * @returns {string} Supported MIME type
     */
    _getSupportedMimeType() {
        const types = [
            'video/webm;codecs=vp9,opus',
            'video/webm;codecs=vp8,opus',
            'video/webm',
            'video/mp4',
            'video/ogg'
        ];

        for (const type of types) {
            if (MediaRecorder.isTypeSupported(type)) {
                return type;
            }
        }

        return this.options.videoMimeType; // Fallback
    }

    /**
     * Show an error message
     * @param {string} message - Error message to display
     */
    _showError(message) {
        if (this.errorMessage) {
            this.errorMessage.textContent = message;
            this.errorMessage.style.display = 'block';
        } else {
            console.error('Error:', message);
        }
    }

    /**
     * Hide the error message
     */
    _hideError() {
        if (this.errorMessage) {
            this.errorMessage.style.display = 'none';
        }
    }

    /**
     * Show a status message
     * @param {string} message - Status message to display
     */
    _showStatus(message) {
        const statusElement = document.querySelector('.status-message');
        if (statusElement) {
            statusElement.textContent = message;
        }
    }

    /**
     * Clean up resources when done
     */
    dispose() {
        // Stop the media stream
        if (this.stream) {
            this.stream.getTracks().forEach(track => track.stop());
        }

        // Release video element resources
        if (this.videoElement) {
            this.videoElement.srcObject = null;
            this.videoElement.src = '';
        }

        // Stop recording if active
        if (this.recording && this.mediaRecorder) {
            this.mediaRecorder.stop();
        }
    }
}

// Initialize WebCamCapture on page load if auto-init is requested
document.addEventListener('DOMContentLoaded', function() {
    // Check if auto-init is requested through data attribute
    const autoInitElement = document.querySelector('[data-webcam-autoinit="true"]');
    if (autoInitElement) {
        window.webCamCapture = new WebCamCapture();
        window.webCamCapture.initialize();
    }
});

// Export for module usage
if (typeof module !== 'undefined') {
    module.exports = WebCamCapture;
}