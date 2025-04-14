import React, { useEffect, useRef, useState, useCallback, forwardRef, useImperativeHandle } from 'react';
import { 
  Box, 
  Button, 
  TextField, 
  Typography, 
  Paper, 
  Stack,
  CircularProgress,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  SelectChangeEvent,
  ButtonGroup,
  Grid,
  Fab,
  Slider,
  Tooltip
} from '@mui/material';
import CameraAltIcon from '@mui/icons-material/CameraAlt';
import VideocamIcon from '@mui/icons-material/Videocam';
import StopIcon from '@mui/icons-material/Stop';
import PauseIcon from '@mui/icons-material/Pause';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SettingsIcon from '@mui/icons-material/Settings';
import TuneIcon from '@mui/icons-material/Tune';
import RefreshIcon from '@mui/icons-material/Refresh';
import InfoIcon from '@mui/icons-material/Info';
import GetAppIcon from '@mui/icons-material/GetApp';

// Define types
export interface WebCamCaptureProps {
  inputValues: Record<string, any>;
  setInputValues: (values: Record<string, any>) => void;
  onSubmit: () => void;
  outputValues: Record<string, any>;
  loading: boolean;
  options?: WebCamCaptureOptions;
}

export interface WebCamCaptureOptions {
  imageFieldName?: string;
  videoFieldName?: string;
  videoMimeTypeFieldName?: string;
  fileNameFieldName?: string;
  showFilenameInput?: boolean;
  aspectRatio?: string;
  maxHeight?: number;
  videoQuality?: 'high' | 'medium' | 'low';
  maxRecordingDuration?: number;
}

export interface WebCamCaptureMethods {
  capturePhoto: () => void;
  startRecording: () => void;
  stopRecording: () => void;
}

interface MediaDevice {
  deviceId: string;
  kind: string;
  label: string;
}

/**
 * WebCamCapture - A reusable camera component for plugins with device selection
 */
const WebCamCapture = forwardRef<WebCamCaptureMethods, WebCamCaptureProps>(({ 
  inputValues, 
  setInputValues, 
  onSubmit, 
  outputValues, 
  loading,
  options = {} 
}, ref) => {
  // Configuration defaults with reactive state
  const [maxDuration, setMaxDuration] = useState(options.maxRecordingDuration || 30); // Increased to 30 seconds
  const [videoQuality, setVideoQuality] = useState(options.videoQuality || 'low'); // Default to low
  const [showAdvancedSettings, setShowAdvancedSettings] = useState(false);
  const [videoCodec, setVideoCodec] = useState<string>('h264'); // Default to h264 as requested
  
  // Configuration defaults
  const config = {
    imageFieldName: 'capturedImageData',
    videoFieldName: 'capturedVideoData',
    videoMimeTypeFieldName: 'capturedVideoMimeType',
    fileNameFieldName: 'outputFileName',
    showFilenameInput: true,
    aspectRatio: '16/9',
    maxHeight: 400,
    videoQuality: videoQuality as 'high' | 'medium' | 'low',
    maxRecordingDuration: maxDuration,
    ...options
  };
  
  // State variables
  const videoRef = useRef<HTMLVideoElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const recordedChunksRef = useRef<Blob[]>([]);
  const lastRecordedBlobRef = useRef<Blob | null>(null);
  const [isRecording, setIsRecording] = useState<boolean>(false);
  const [isPaused, setIsPaused] = useState<boolean>(false);
  const [cameraReady, setCameraReady] = useState<boolean>(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [recordingTime, setRecordingTime] = useState<number>(0);
  const timerRef = useRef<number | null>(null);
  const [showDeviceSelector, setShowDeviceSelector] = useState<boolean>(false);
  const [recordingInProgress, setRecordingInProgress] = useState<boolean>(false);
  const [submissionAttemptCount, setSubmissionAttemptCount] = useState<number>(0);
  const lastVideoSizeRef = useRef<number>(0);
  const processingTimeoutRef = useRef<number | null>(null);
  
  // Device selection state
  const [videoDevices, setVideoDevices] = useState<MediaDevice[]>([]);
  const [audioDevices, setAudioDevices] = useState<MediaDevice[]>([]);
  const [selectedVideoDevice, setSelectedVideoDevice] = useState<string>('');
  const [selectedAudioDevice, setSelectedAudioDevice] = useState<string>('');

  // CRITICAL: Force reset recording state on backend error
  useEffect(() => {
    if (outputValues && outputValues.errorMessage) {
      if (recordingInProgress) {
        console.warn("Backend error detected:", outputValues.errorMessage);
        setRecordingInProgress(false);
        if (outputValues.errorMessage.includes("No image or video data received")) {
          setCameraError("Server cannot process the video. Trying screenshot fallback.");
          if (lastRecordedBlobRef.current) {
            console.log("Trying to extract a screenshot from video due to backend error");
            extractScreenshotFromVideo(lastRecordedBlobRef.current);
          }
        } else {
             setCameraError(`Backend Error: ${outputValues.errorMessage}. Please try again.`);
        }
      }
    } else if (outputValues && outputValues.success) {
      setSubmissionAttemptCount(0);
      setRecordingInProgress(false);
      console.log("Video processing successful");
    }
  }, [outputValues]); // Added extractScreenshotFromVideo to dependency array if needed, but it's stable via useCallback
  
  // Clear processing state if it gets stuck for too long
  useEffect(() => {
    if (recordingInProgress) {
      if (processingTimeoutRef.current) {
        clearTimeout(processingTimeoutRef.current);
      }
      processingTimeoutRef.current = window.setTimeout(() => {
        console.warn("Processing timeout reached, resetting state");
        setRecordingInProgress(false);
        setCameraError("Processing timed out. Trying screenshot fallback.");
        if (lastRecordedBlobRef.current) {
             extractScreenshotFromVideo(lastRecordedBlobRef.current);
        }
      }, 20000); // 20 second timeout
      
      return () => {
        if (processingTimeoutRef.current) {
          clearTimeout(processingTimeoutRef.current);
        }
      };
    }
  }, [recordingInProgress]); // Added extractScreenshotFromVideo to dependency array if needed

  // Extract a screenshot from a video blob
  const extractScreenshotFromVideo = useCallback((videoBlob: Blob) => {
    console.log("Attempting to extract screenshot from video blob", videoBlob);
    try {
      const videoUrl = URL.createObjectURL(videoBlob);
      const video = document.createElement('video');
      video.muted = true; // Mute to avoid potential autoplay issues
      video.playsInline = true; // Important for mobile
      
      video.onloadedmetadata = () => {
        console.log("Video metadata loaded for screenshot extraction:", video.videoWidth, video.videoHeight);
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth || 640;
        canvas.height = video.videoHeight || 480;
        const ctx = canvas.getContext('2d');
        
        if (!ctx) {
          console.error("Could not get canvas context for screenshot");
          setCameraError("Could not extract screenshot (canvas error). Please try taking a photo instead.");
          setRecordingInProgress(false); // Ensure progress stops
          URL.revokeObjectURL(videoUrl); // Clean up URL
          return;
        }
        
        video.currentTime = 0.1; // Seek slightly into the video for potentially better frame

        video.onseeked = () => {
          console.log("Video seeked for screenshot extraction");
          try {
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
            const imageDataURL = canvas.toDataURL('image/jpeg', 0.85); // Slightly lower quality for smaller size
            const base64Image = imageDataURL.split(',')[1];
            
            console.log(`Extracted screenshot from video, size: ${Math.round(base64Image.length / 1024)} KB`);
            
            const updatedValues = { ...inputValues };
            updatedValues[config.imageFieldName] = base64Image;
            updatedValues[config.videoFieldName] = ''; 
            updatedValues[config.videoMimeTypeFieldName] = '';
            
            if (config.fileNameFieldName && !inputValues[config.fileNameFieldName]) {
              updatedValues[config.fileNameFieldName] = "Screenshot_from_video";
            }
            
            setInputValues(updatedValues);
            console.log("Submitting screenshot extracted from video");
            onSubmit(); // This should trigger the loading state via the parent
            // Do NOT setRecordingInProgress(false) here - let the onSubmit flow handle it via outputValues useEffect
            URL.revokeObjectURL(videoUrl); // Clean up URL

          } catch (e) {
            console.error("Error during canvas toDataURL or submission:", e);
            setCameraError("Could not extract screenshot (conversion error). Please try taking a photo instead.");
            setRecordingInProgress(false);
            URL.revokeObjectURL(videoUrl); // Clean up URL
          }
        };

        video.onerror = (e) => {
           console.error("Error during video seeking for screenshot:", e);
           setCameraError("Could not seek video for screenshot. Please try again.");
           setRecordingInProgress(false);
           URL.revokeObjectURL(videoUrl); // Clean up URL
        };

        // If seeked doesn't fire, try drawing immediately after play starts
        video.onplaying = () => {
            if (!video.seeking) { // Ensure we haven't already triggered via onseeked
                console.log("Drawing screenshot on 'playing' event");
                 try {
                    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
                    const imageDataURL = canvas.toDataURL('image/jpeg', 0.85);
                    const base64Image = imageDataURL.split(',')[1];
                    
                    const updatedValues = { ...inputValues };
                    updatedValues[config.imageFieldName] = base64Image;
                    updatedValues[config.videoFieldName] = ''; 
                    updatedValues[config.videoMimeTypeFieldName] = '';
                    
                    if (config.fileNameFieldName && !inputValues[config.fileNameFieldName]) {
                        updatedValues[config.fileNameFieldName] = "Screenshot_from_video";
                    }
                    
                    setInputValues(updatedValues);
                    console.log("Submitting screenshot extracted from video (onplaying)");
                    onSubmit();
                    URL.revokeObjectURL(videoUrl);
                } catch (e) {
                    console.error("Error during canvas toDataURL (onplaying):", e);
                    setCameraError("Could not extract screenshot (conversion error). Please try taking a photo instead.");
                    setRecordingInProgress(false);
                    URL.revokeObjectURL(videoUrl);
                }
            }
        }
      };
      
      video.onerror = (e) => {
        console.error("Error loading video for screenshot:", e);
        setCameraError("Could not load video for screenshot. Please try again.");
        setRecordingInProgress(false);
        URL.revokeObjectURL(videoUrl); // Clean up URL
      };
      
      video.src = videoUrl;
      video.load(); // Explicitly load
      video.play().catch(err => {
          console.warn("Video play() for screenshot failed (might be okay if onloadedmetadata works):", err);
          // If play fails, sometimes seeking manually after load works
          if(!video.onseeked) { // Only if not already handled
             video.currentTime = 0.1;
          }
      });
      
    } catch (e) {
      console.error("General error in screenshot extraction:", e);
      setCameraError("Could not process video for screenshot. Please try taking a photo instead.");
      setRecordingInProgress(false);
    }
  }, [inputValues, config, setInputValues, onSubmit]); // Include setInputValues and onSubmit

  const handleDownload = useCallback(() => {
    try {
      console.log("Download requested", {
        outputValues: outputValues,
        lastBlobExists: !!lastRecordedBlobRef.current,
        // Note: inputValues might be cleared by parent, less reliable here
        // hasInputImageData: !!inputValues[config.imageFieldName],
        // hasInputVideoData: !!inputValues[config.videoFieldName]
      });
  
      // --- Primary Path: Backend Confirmed Success ---
      if (outputValues.success && outputValues.savedFileName) {
        console.log("Using backend-provided file information for download. FileType:", outputValues.savedFileType);
  
        // Option A: Backend provides a direct download URL (Most Robust)
        if (outputValues.downloadUrl) {
          console.log("Using backend download URL:", outputValues.downloadUrl);
          const link = document.createElement('a');
          link.href = outputValues.downloadUrl;
          link.download = outputValues.savedFileName; // Suggest filename
          document.body.appendChild(link);
          link.click();
          setTimeout(() => document.body.removeChild(link), 100);
          return;
        }
  
        // Option B: Download client-side data based on saved file type
  
        // === IMAGE DOWNLOAD ===
        if (outputValues.savedFileType === 'image') {
          let imageData = null;
          // Prefer data URL from outputValues.capturedImagePreview
          if (outputValues.capturedImagePreview && outputValues.capturedImagePreview.startsWith('data:')) {
            imageData = outputValues.capturedImagePreview;
            console.log("Using capturedImagePreview data URL for image download.");
          }
          // Fallback: Try adding prefix if preview is just base64
          else if (outputValues.capturedImagePreview) {
             imageData = `data:image/jpeg;base64,${outputValues.capturedImagePreview}`;
             console.log("Using capturedImagePreview (prefixed) for image download.");
          }
          // Less likely fallback: Check inputValues (parent might clear this)
          else if (inputValues[config.imageFieldName]) {
             imageData = `data:image/jpeg;base64,${inputValues[config.imageFieldName]}`;
             console.warn("Using inputValues image data for download (may be unreliable).");
          }
  
          if (imageData) {
            const link = document.createElement('a');
            link.href = imageData;
            link.download = outputValues.savedFileName;
            document.body.appendChild(link);
            link.click();
            setTimeout(() => document.body.removeChild(link), 100);
            console.log("Image download initiated.");
            return;
          } else {
            // If no image data found client-side, error out
            throw new Error(`Client-side image data not found for ${outputValues.savedFileName}. Cannot download directly.`);
          }
        }
  
        // === VIDEO DOWNLOAD ===
        else if (outputValues.savedFileType === 'video') {
          // **PRIORITY 1: Use the stored Blob reference**
          if (lastRecordedBlobRef.current) {
            console.log("Attempting video download using lastRecordedBlobRef (Blob).");
            try {
              const blob = lastRecordedBlobRef.current;
              const url = URL.createObjectURL(blob);
              const link = document.createElement('a');
              link.href = url;
              // Use backend filename, derive extension from blob type as fallback
              link.download = outputValues.savedFileName || `captured_video.${blob.type.split('/')[1] || 'webm'}`;
              document.body.appendChild(link);
              link.click();
              setTimeout(() => {
                document.body.removeChild(link);
                URL.revokeObjectURL(url);
              }, 100);
              console.log("Blob download initiated for video.");
              return; // Success!
            } catch (blobError) {
              console.error("Error creating download from blob:", blobError);
              // Don't throw yet, try fallback below
            }
          }
  
          // **PRIORITY 2: Fallback to Base64 data (if parent passes it back or doesn't clear input)**
          const videoBase64 = outputValues.videoData || inputValues[config.videoFieldName]; // Check both
          const mimeType = outputValues.videoMimeType || inputValues[config.videoMimeTypeFieldName];
  
          if (videoBase64 && mimeType) {
             console.warn("Attempting video download using Base64 data (Fallback).");
             try {
                // Convert base64 to Blob
                const byteCharacters = atob(videoBase64);
                const byteArrays = [];
                for (let offset = 0; offset < byteCharacters.length; offset += 512) {
                    const slice = byteCharacters.slice(offset, offset + 512);
                    const byteNumbers = new Array(slice.length);
                    for (let i = 0; i < slice.length; i++) {
                        byteNumbers[i] = slice.charCodeAt(i);
                    }
                    const byteArray = new Uint8Array(byteNumbers);
                    byteArrays.push(byteArray);
                }
                const blob = new Blob(byteArrays, { type: mimeType });
                const url = URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.download = outputValues.savedFileName || `captured_video.${mimeType.includes('mp4') ? 'mp4' : 'webm'}`;
                document.body.appendChild(link);
                link.click();
                setTimeout(() => {
                    document.body.removeChild(link);
                    URL.revokeObjectURL(url);
                }, 100);
                console.log("Base64 download initiated for video.");
                return; // Success!
             } catch (base64Error) {
                console.error("Error creating download from base64:", base64Error);
                // Throw error if fallback fails
                throw new Error(`Failed to download video from Base64 data for ${outputValues.savedFileName}.`);
             }
          }
  
          // If neither Blob nor Base64 worked:
          throw new Error(`Client-side video data (Blob or Base64) not found for ${outputValues.savedFileName}. Cannot download directly.`);
  
        } else {
          // Unknown file type from backend
           throw new Error(`Unknown file type '${outputValues.savedFileType}' reported by server for ${outputValues.savedFileName}.`);
        }
      }
  
      // --- Fallback Path: No Backend Success Confirmation (Less Common) ---
      // This path is less likely if your flow relies on backend success
      else if (lastRecordedBlobRef.current) { // Check blob first for video
         console.warn("Attempting download using local Blob (no backend success confirmed).");
         // ... (duplicate blob download logic from above) ...
          try {
              const blob = lastRecordedBlobRef.current;
              const url = URL.createObjectURL(blob);
              const link = document.createElement('a');
              link.href = url;
              const extension = blob.type.split('/')[1] || 'webm';
              link.download = inputValues[config.fileNameFieldName] || `local_video_${Date.now()}.${extension}`;
              document.body.appendChild(link);
              link.click();
              setTimeout(() => { /* cleanup */ }, 100);
              return;
          } catch (blobError) { /* handle error */ }
  
      } else if (inputValues[config.imageFieldName]) {
        console.warn("Attempting download using local image input data (no backend success confirmed).");
        // ... (duplicate image download logic using inputValues[config.imageFieldName]) ...
         try {
              const imageData = `data:image/jpeg;base64,${inputValues[config.imageFieldName]}`;
              const link = document.createElement('a');
              link.href = imageData;
              link.download = inputValues[config.fileNameFieldName] || `local_image_${Date.now()}.jpg`;
              document.body.appendChild(link);
              link.click();
              setTimeout(() => { /* cleanup */ }, 100);
              return;
         } catch (imgError) { /* handle error */ }
  
      } else if (inputValues[config.videoFieldName] && inputValues[config.videoMimeTypeFieldName]) {
         console.warn("Attempting download using local video input data (no backend success confirmed).");
         // ... (duplicate base64 video download logic using inputValues) ...
         try {
              // ... base64 conversion ...
              const blob = new Blob(byteArrays, { type: inputValues[config.videoMimeTypeFieldName] });
              const url = URL.createObjectURL(blob);
              const link = document.createElement('a');
              link.href = url;
              const extension = inputValues[config.videoMimeTypeFieldName].includes('mp4') ? 'mp4' : 'webm';
              link.download = inputValues[config.fileNameFieldName] || `local_video_${Date.now()}.${extension}`;
              document.body.appendChild(link);
              link.click();
              setTimeout(() => { /* cleanup */ }, 100);
              return;
         } catch(base64Error) { /* handle error */ }
      }
  
      // --- Final Error ---
      throw new Error("No downloadable media found (checked backend success and local data). Try capturing again.");
  
    } catch (error: any) { // Catch potential errors thrown within the try block
      console.error("Download error:", error);
      // Display temporary toast message
      const errorToast = document.createElement('div');
      // ... (styling for errorToast as before) ...
      errorToast.style.position = 'fixed'; /* ... other styles */
      errorToast.textContent = `Download failed: ${error.message}`;
      document.body.appendChild(errorToast);
      setTimeout(() => document.body.removeChild(errorToast), 5000);
    }
  // Re-evaluate dependencies - refs don't need to be listed. inputValues and outputValues are key.
  // config is stable if defined outside. setCameraError/extractScreenshotFromVideo might be needed if used.
  }, [outputValues, inputValues, config]); // Removed refs and unused callbacks from deps

// Process the recorded video with a reliable approach
const processRecording = useCallback(() => {
  if (recordedChunksRef.current.length === 0) {
    console.error("No chunks to process");
    setCameraError("No video data was recorded. Please try again.");
    setRecordingInProgress(false);
    recordedChunksRef.current = [];
    return;
  }

  try {
    console.log(`Processing recording with ${recordedChunksRef.current.length} chunks...`);
    
    // Create a blob from all chunks
    const mimeType = mediaRecorderRef.current?.mimeType || 'video/webm';
    const blob = new Blob(recordedChunksRef.current, { type: mimeType });
    
    // Store the blob for potential retry
    lastRecordedBlobRef.current = blob;
    lastVideoSizeRef.current = blob.size;
    recordedChunksRef.current = [];
    
    console.log(`Created video blob: Size=${blob.size} bytes, Type=${mimeType}`);
    
    if (blob.size < 1000) {
      setCameraError("Recorded video is too small. Please try again.");
      setRecordingInProgress(false);
      return;
    }
    
    // RELIABLE VIDEO PROCESSING: ARRAY BUFFER + BINARY STRING APPROACH
    console.log("Using reliable binary processing approach for video");
    
    // Step 1: Convert blob to ArrayBuffer
    const fileReader = new FileReader();
    
    fileReader.onload = (event) => {
      if (!event.target || !event.target.result) {
        console.error("FileReader failed to load blob as ArrayBuffer");
        extractScreenshotFromVideo(blob);
        return;
      }
      
      try {
        // Step 2: Get the ArrayBuffer result
        const arrayBuffer = event.target.result as ArrayBuffer;
        
        // Step 3: Convert ArrayBuffer to Binary String (more reliable than direct base64)
        const binaryString = Array.from(new Uint8Array(arrayBuffer))
          .map(byte => String.fromCharCode(byte))
          .join('');
        
        // Step 4: Convert Binary String to Base64
        const base64 = window.btoa(binaryString);
        
        console.log(`Reliable conversion complete. Original size: ${blob.size} bytes, Base64 length: ${base64.length} chars`);
        
        // Verify we have reasonable data
        if (base64.length < blob.size * 0.5) {
          console.error(`Base64 data too small: ${base64.length} chars from ${blob.size} byte blob`);
          extractScreenshotFromVideo(blob);
          return;
        }
        
        // Submit the video data with additional safety check
        const updatedValues = { ...inputValues };
        
        // BUGFIX: Double-check that image field is empty before submitting video
        // This ensures any previous photo data won't override our video submission
        if (config.imageFieldName && updatedValues[config.imageFieldName]) {
          console.warn("Image data still present before video submission - forcing clear");
          updatedValues[config.imageFieldName] = '';
        }
        
        updatedValues[config.videoFieldName] = base64;
        updatedValues[config.videoMimeTypeFieldName] = mimeType;
        
        // Optionally update output filename if provided
        if (config.fileNameFieldName && inputValues[config.fileNameFieldName]) {
          const filename = inputValues[config.fileNameFieldName];
          // Keep the filename as is, backend will add extension
        }
        
        setInputValues(updatedValues);
        console.log("Submitting video as base64 data with length:", base64.length);
        onSubmit();
      } catch (e) {
        console.error("Error in ArrayBuffer processing:", e);
        extractScreenshotFromVideo(blob);
      }
    };
    
    fileReader.onerror = (error) => {
      console.error("FileReader error during ArrayBuffer read:", error);
      extractScreenshotFromVideo(blob);
    };
    
    // Read as ArrayBuffer instead of DataURL
    fileReader.readAsArrayBuffer(blob);
    
  } catch (e) {
    console.error("Error in processRecording:", e);
    setCameraError("Error processing recording. Please try again.");
    setRecordingInProgress(false);
    // Try fallback if possible
    if (lastRecordedBlobRef.current) {
      extractScreenshotFromVideo(lastRecordedBlobRef.current);
    }
  }
}, [inputValues, config, extractScreenshotFromVideo, setInputValues, onSubmit]);

  // Stop video recording
  const stopRecording = useCallback(() => {
    console.log("Attempting to stop recording...");
    
    if (timerRef.current !== null) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    
    setIsRecording(false);
    setIsPaused(false);
    setRecordingTime(0); // Reset timer display

    if (!mediaRecorderRef.current) {
      console.warn("No media recorder instance found to stop.");
      // If we somehow have chunks without a recorder, try processing
       if (recordedChunksRef.current.length > 0) {
            console.warn("No recorder, but chunks exist. Attempting processing.");
            setRecordingInProgress(true);
            processRecording();
        } else {
            setRecordingInProgress(false);
        }
      return;
    }
    
    try {
      const recorderState = mediaRecorderRef.current.state;
      console.log(`MediaRecorder state before stop attempt: ${recorderState}`);
      
      if (recorderState === 'recording' || recorderState === 'paused') {
        // Add a final listener for data before stopping
        // This helps catch the last chunk if stop() is called immediately
        const handleLastData = (e: BlobEvent) => {
             if (e.data && e.data.size > 0) {
                console.log(`Received final chunk on stop: ${e.data.size} bytes`);
                recordedChunksRef.current.push(e.data);
            }
            // Remove this listener after it fires once
            mediaRecorderRef.current?.removeEventListener('dataavailable', handleLastData);

            // Now proceed with processing inside the main onstop handler
            // (onstop should fire after this)
        }
        mediaRecorderRef.current.addEventListener('dataavailable', handleLastData);

        // Request data explicitly just before stop - belt and suspenders
        if (recorderState === 'recording') {
             mediaRecorderRef.current.requestData();
             console.log("Final data chunk requested before stop");
        }

        // Stop the recorder
        mediaRecorderRef.current.stop();
        console.log("MediaRecorder.stop() called.");
        // Processing should happen in the onstop handler now.
        
      } else if (recorderState === 'inactive') {
        console.log("Recorder is already inactive.");
        // Process any chunks that might have been collected before it became inactive
        if (recordedChunksRef.current.length > 0) {
          console.log("Processing existing chunks from inactive recorder state...");
          setRecordingInProgress(true);
          processRecording();
        } else {
          console.log("No chunks to process from inactive recorder.");
          setRecordingInProgress(false); // Ensure overlay is hidden
        }
      }
    } catch (error) {
      console.error("Error during stopRecording:", error);
      setCameraError("Error stopping recording. Trying to process existing data.");
      if (recordedChunksRef.current.length > 0) {
        setRecordingInProgress(true);
        processRecording();
      } else {
        setRecordingInProgress(false);
      }
    }
  }, [processRecording]); // processRecording is stable via useCallback

  // Expose methods to parent component via ref
  useImperativeHandle(ref, () => ({
    capturePhoto: () => capturePhoto(),
    startRecording: () => startRecording(),
    stopRecording: () => stopRecording()
  }));

  // Load available media devices
  const loadMediaDevices = useCallback(async () => {
    try {
      // Ensure permissions first (might have been revoked)
      await navigator.mediaDevices.getUserMedia({ video: true, audio: true }); 
      const devices = await navigator.mediaDevices.enumerateDevices();
      
      const videoInputs = devices.filter(device => device.kind === 'videoinput');
      const audioInputs = devices.filter(device => device.kind === 'audioinput');
      
      setVideoDevices(videoInputs.map((device, index) => ({ // Added index for fallback label
        deviceId: device.deviceId,
        kind: device.kind,
        label: device.label || `Camera ${index + 1}`
      })));
      
      setAudioDevices(audioInputs.map((device, index) => ({ // Added index for fallback label
        deviceId: device.deviceId,
        kind: device.kind,
        label: device.label || `Microphone ${index + 1}`
      })));
      
      // Only set default if not already selected or if previous selection is gone
       if (videoInputs.length > 0 && (!selectedVideoDevice || !videoInputs.some(d => d.deviceId === selectedVideoDevice))) {
            setSelectedVideoDevice(videoInputs[0].deviceId);
        } else if (videoInputs.length === 0) {
            setSelectedVideoDevice(''); // Clear selection if no devices
        }

        if (audioInputs.length > 0 && (!selectedAudioDevice || !audioInputs.some(d => d.deviceId === selectedAudioDevice))) {
            setSelectedAudioDevice(audioInputs[0].deviceId);
        } else if (audioInputs.length === 0) {
             setSelectedAudioDevice(''); // Clear selection if no devices
        }
      
    } catch (error: any) {
      console.error('Error loading media devices:', error);
      setCameraError(`Could not access media devices: ${error.message}. Please check browser permissions.`);
       setCameraReady(false); // Ensure camera is not marked ready
    }
  }, [selectedVideoDevice, selectedAudioDevice]); // Re-run if selection changes externally? Maybe not needed.

  // Get video constraints based on quality setting
  const getVideoConstraints = useCallback(() => {
    const baseConstraints: MediaTrackConstraints = { // Added type
      deviceId: selectedVideoDevice ? { exact: selectedVideoDevice } : undefined
    };
    
    const qualityConstraints: MediaTrackConstraints = (() => { // Added type
      switch(config.videoQuality) {
        case 'high': return { width: { ideal: 1280 }, height: { ideal: 720 }, frameRate: { ideal: 30 } };
        case 'medium': return { width: { ideal: 854 }, height: { ideal: 480 }, frameRate: { ideal: 24 } };
        case 'low': 
        default: return { width: { ideal: 640 }, height: { ideal: 360 }, frameRate: { ideal: 15 } }; // Lower frame rate for low quality
      }
    })();
    
    return { ...baseConstraints, ...qualityConstraints };
  }, [config.videoQuality, selectedVideoDevice]);

  // Get audio constraints
  const getAudioConstraints = useCallback(() => {
    return {
      deviceId: selectedAudioDevice ? { exact: selectedAudioDevice } : undefined,
      echoCancellation: true,
      noiseSuppression: true,
      autoGainControl: true // Often helpful
    };
  }, [selectedAudioDevice]);

  // Initialize or restart webcam with selected devices
  const initializeCamera = useCallback(async () => {
    // Only proceed if we have at least a video device selected (or default)
    if (!selectedVideoDevice && videoDevices.length > 0) {
        console.log("Waiting for default video device selection...");
        return; // Don't initialize if selection isn't stable yet
    }
     if (videoDevices.length === 0) {
        console.warn("No video devices available to initialize.");
        setCameraError("No video input devices found. Please connect a camera.");
        setCameraReady(false);
        return;
     }


    console.log("Attempting to initialize camera...");
    setCameraError(null);
    setCameraReady(false);
    
    // Stop any existing stream FIRST
    if (streamRef.current) {
      console.log("Stopping previous stream tracks...");
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
     // Also ensure video element src is cleared
    if (videoRef.current) {
        videoRef.current.srcObject = null;
    }
    
    try {
      const constraints = {
        video: getVideoConstraints(),
        // Only request audio if a device is selected or available
        audio: (selectedAudioDevice || audioDevices.length > 0) ? getAudioConstraints() : false 
      };
      console.log("Requesting media stream with constraints:", JSON.stringify(constraints));
      
      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      console.log("Media stream obtained successfully.");
      streamRef.current = stream;
      
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        // Wait for video to start playing to set ready state
        videoRef.current.onloadedmetadata = () => {
             console.log("Video metadata loaded, camera should be ready.");
             setCameraReady(true);
        }
         videoRef.current.onplaying = () => {
             console.log("Video element started playing.");
             setCameraReady(true); // Set ready when it actually plays
        }
        // Add error handling for the video element itself
         videoRef.current.onerror = (e) => {
            console.error("Video element error:", e);
            setCameraError("There was an error displaying the camera feed.");
            setCameraReady(false);
         }

      } else {
         console.warn("Video ref not available after getting stream.");
         // Clean up stream if we can't display it
          stream.getTracks().forEach(track => track.stop());
          streamRef.current = null;
          setCameraError("Could not attach camera stream to video element.");
      }
      
      console.log("Camera initialized with devices:", 
        { video: selectedVideoDevice, audio: selectedAudioDevice || 'default/none' });
      
    } catch (error: any) {
      console.error('Camera initialization error:', error.name, error.message, error);
      let specificError = `Camera access error: ${error.message || 'Could not access camera'}`;
      if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
          specificError = "Permission Denied: Please allow camera and microphone access in your browser settings.";
      } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
          specificError = "Camera Not Found: No camera or microphone device was found. Please ensure they are connected and enabled.";
      } else if (error.name === 'NotReadableError' || error.name === 'TrackStartError') {
           specificError = "Hardware Error: The camera or microphone might be in use by another application or temporarily unavailable.";
      } else if (error.name === 'OverconstrainedError') {
           specificError = `Device capabilities mismatch: The selected camera/settings (${config.videoQuality}) are not supported. Try lower quality or a different camera. Requested: ${JSON.stringify(getVideoConstraints())}`;
      }
      setCameraError(specificError);
      setCameraReady(false); // Ensure not ready on error
    }
  }, [selectedVideoDevice, selectedAudioDevice, getVideoConstraints, getAudioConstraints, videoDevices.length, config.videoQuality]); // Added dependencies

  // Load devices on mount
  useEffect(() => {
    loadMediaDevices();
    navigator.mediaDevices.addEventListener('devicechange', loadMediaDevices);
    return () => {
      navigator.mediaDevices.removeEventListener('devicechange', loadMediaDevices);
      // Ensure stream is stopped on unmount
       if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop());
      }
       if (timerRef.current !== null) {
        clearInterval(timerRef.current);
      }
        if (processingTimeoutRef.current !== null) {
            clearTimeout(processingTimeoutRef.current);
        }
    };
  }, [loadMediaDevices]); // loadMediaDevices is stable

  // Initialize camera when devices are loaded/selected, or constraints change
  useEffect(() => {
    // Initialize only if we have devices listed and a video device is selected (or trying default)
    if (videoDevices.length > 0 && (selectedVideoDevice || videoDevices.length === 1) ) {
        // Debounce initialization slightly to avoid rapid restarts on quick changes
        const timerId = setTimeout(() => {
             initializeCamera();
        }, 100); // 100ms debounce
       
        return () => clearTimeout(timerId);
    } else if (videoDevices.length === 0 && cameraError === null) {
       // If devices disappear, show error
       // setCameraError("No video devices detected."); // This might conflict with loadMediaDevices error
    }
  }, [selectedVideoDevice, selectedAudioDevice, videoDevices, initializeCamera, cameraError]); // Added cameraError to prevent loop if error occurs during init

  // Handle video device change
  const handleVideoDeviceChange = (event: SelectChangeEvent) => {
    setSelectedVideoDevice(event.target.value);
    setCameraReady(false); // Force re-init
    setCameraError(null); // Clear previous errors
  };

  // Handle audio device change
  const handleAudioDeviceChange = (event: SelectChangeEvent) => {
    setSelectedAudioDevice(event.target.value);
     setCameraReady(false); // Force re-init
     setCameraError(null);
  };

  // Handle video quality change
  const handleQualityChange = (event: SelectChangeEvent) => {
    setVideoQuality(event.target.value as 'high' | 'medium' | 'low');
     setCameraReady(false); // Re-init might be needed if constraints change drastically
     setCameraError(null);
  };

// Handle taking a photo
const capturePhoto = useCallback(() => {
  if (!videoRef.current || !cameraReady || !streamRef.current) {
    setCameraError("Camera is not ready for photo capture.");
    return;
  }
  
  try {
    console.log("Capturing photo...");
    const videoTrack = streamRef.current.getVideoTracks()[0];
    const settings = videoTrack?.getSettings();
    const canvas = document.createElement('canvas');
    // Use actual video dimensions if available, otherwise fallback
    canvas.width = settings?.width || videoRef.current.videoWidth || 640; 
    canvas.height = settings?.height || videoRef.current.videoHeight || 480;
    
    const context = canvas.getContext('2d');
    if (!context) throw new Error("Could not get canvas context");
    
    // Flip image horizontally if it's a front-facing camera (heuristic)
    const isFrontFacing = videoTrack?.label?.toLowerCase().includes('front');
    if (isFrontFacing) {
        context.translate(canvas.width, 0);
        context.scale(-1, 1);
    }

    context.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);
    
    const imageDataURL = canvas.toDataURL('image/jpeg', 0.9); // Quality 0.9
    const base64Data = imageDataURL.split(',')[1]; 
    
    console.log(`Photo captured, Base64 length: ${base64Data.length}, Approx Size: ${Math.round(base64Data.length * 3 / 4 / 1024)} KB`);
    
    const updatedValues = { ...inputValues };
    updatedValues[config.imageFieldName] = base64Data;
    
    // Explicitly clear any video data to prevent confusion
    if (config.videoFieldName) updatedValues[config.videoFieldName] = '';
    if (config.videoMimeTypeFieldName) updatedValues[config.videoMimeTypeFieldName] = '';
    
    setInputValues(updatedValues);
    
    console.log("Submitting photo data to backend");
    onSubmit();
    
  } catch (error: any) {
    console.error('Error capturing photo:', error);
    setCameraError(`Photo capture error: ${error.message}`);
  }
}, [cameraReady, inputValues, onSubmit, setInputValues, config]); // Added dependencies

  // Toggle pause/resume recording
  const togglePause = useCallback(() => {
    if (!mediaRecorderRef.current || !isRecording || recordingInProgress) return; // Prevent action during processing

    try {
      if (isPaused) {
        if (mediaRecorderRef.current.state === 'paused') {
          mediaRecorderRef.current.resume();
          console.log("Resumed recording. State:", mediaRecorderRef.current.state);
          setIsPaused(false); // Update state after successful resume
            // Resume timer
            if (timerRef.current === null) { // Should be null if paused
            timerRef.current = window.setInterval(() => {
                setRecordingTime(prev => {
                const newTime = prev + 1;
                if (newTime >= config.maxRecordingDuration) {
                    stopRecording(); // Stop immediately
                    return config.maxRecordingDuration;
                }
                return newTime;
                });
            }, 1000);
            }
        } else {
            console.warn("Attempted to resume, but recorder state is not 'paused':", mediaRecorderRef.current.state);
        }
      } else {
        if (mediaRecorderRef.current.state === 'recording') {
          mediaRecorderRef.current.pause();
          console.log("Paused recording. State:", mediaRecorderRef.current.state);
          setIsPaused(true); // Update state after successful pause
             // Pause timer
            if (timerRef.current !== null) {
            window.clearInterval(timerRef.current);
            timerRef.current = null;
            }
        } else {
            console.warn("Attempted to pause, but recorder state is not 'recording':", mediaRecorderRef.current.state);
        }
      }
      
    } catch (error) {
      console.error("Error toggling pause state:", error);
      setCameraError("Error pausing/resuming recording.");
    }
  }, [isPaused, isRecording, config.maxRecordingDuration, stopRecording, recordingInProgress]);

// Start video recording
const startRecording = useCallback(() => {
  if (!streamRef.current || !cameraReady) {
    setCameraError("Camera is not ready. Please ensure permissions are granted and try again.");
    initializeCamera(); // Try to re-initialize
    return;
  }
  if (isRecording || recordingInProgress) {
    console.warn("Start recording called while already recording or processing.");
    return; // Prevent multiple recordings or starting while processing
  }
  
  setCameraError(null);
  setRecordingInProgress(false); // Ensure processing overlay is off
  setIsPaused(false);
  lastRecordedBlobRef.current = null; // Clear previous blob
  recordedChunksRef.current = []; // Clear previous chunks
  setSubmissionAttemptCount(0); // Reset attempts

  // Clear ALL previous capture data - both image and video
  // This is important to fix the bug when switching from photo to video mode
  const updatedValues = { ...inputValues };
  
  // Clear image data explicitly to prevent confusion with backend
  if (config.imageFieldName) {
    updatedValues[config.imageFieldName] = '';
    console.log("Cleared previous image data before recording");
  }
  
  // Clear video data
  if (config.videoFieldName) {
    updatedValues[config.videoFieldName] = '';
  }
  
  if (config.videoMimeTypeFieldName) {
    updatedValues[config.videoMimeTypeFieldName] = '';
  }
  
  // Update the inputValues with the cleared data
  setInputValues(updatedValues);

  // Updated supportedMimeTypes based on selected codec
  const supportedMimeTypes = (() => {
    switch(videoCodec) {
      case 'h264':
        return [
          'video/mp4;codecs=h264,aac',
          'video/webm;codecs=h264,opus',
        ];
      case 'vp9':
        return [
          'video/webm;codecs=vp9,opus',
        ];
      case 'vp8':
        return [
          'video/webm;codecs=vp8,opus',
        ];
      default:
        return [
          'video/mp4;codecs=h264,aac', // Default back to h264
          'video/webm;codecs=vp9,opus',
          'video/webm;codecs=vp8,opus',
          'video/webm',
        ];
    }
  })();

  const supportedType = supportedMimeTypes.find(type => MediaRecorder.isTypeSupported(type));

  if (!supportedType && !MediaRecorder.isTypeSupported('')) { // Check default if specific types fail
    setCameraError("Your browser doesn't support MediaRecorder video recording in compatible formats. Try taking a photo instead.");
    console.error("MediaRecorder not supported or no compatible mime types found.");
    return;
  }
   console.log(`Browser supports MediaRecorder. Found compatible type: ${supportedType || 'default'}`);

  try {
    console.log("Starting recording sequence...");
    
    // *** FIX 4: Force Lower Quality (via Bitrate) START ***
    // Using more conservative bitrates, especially lower audio bitrate.
    const options: MediaRecorderOptions = {
      // Use the determined supported type if found, otherwise let browser decide
      mimeType: supportedType || undefined, 
      videoBitsPerSecond: videoQuality === 'low' ? 250000 : // 250 kbps
                          videoQuality === 'medium' ? 500000 : // 500 kbps
                          1000000, // 1 Mbps (High) - still conservative
      // Significantly lower audio bitrate for reliability
      audioBitsPerSecond: 32000, // 32 kbps (was 64000) 
    };
    // *** FIX 4: Force Lower Quality (via Bitrate) END ***
    
    console.log("Attempting to create MediaRecorder with options:", options);
    
    let recorder: MediaRecorder;
    try {
        recorder = new MediaRecorder(streamRef.current, options);
        console.log(`MediaRecorder created successfully. MimeType: ${recorder.mimeType}, VideoBPS: ${recorder.videoBitsPerSecond}, AudioBPS: ${recorder.audioBitsPerSecond}`);
    } catch (creationError: any) {
        console.warn(`Error creating MediaRecorder with specified options (${creationError.message}). Trying default.`);
        // Fallback to default settings if specific options fail
        try {
           recorder = new MediaRecorder(streamRef.current);
           console.log(`Fallback MediaRecorder created. MimeType: ${recorder.mimeType}`);
        } catch (fallbackError: any) {
            console.error("Failed to create MediaRecorder even with default settings:", fallbackError);
            setCameraError(`Failed to initialize recorder: ${fallbackError.message}. Try a different browser or device.`);
            return;
        }
    }

    mediaRecorderRef.current = recorder;

    // Event Handlers (defined before start)
    recorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) {
        console.log(`Chunk received: Size=${e.data.size}, Type=${e.data.type}`);
        recordedChunksRef.current.push(e.data);
      } else {
        console.warn("ondataavailable event fired with empty data chunk.");
      }
    };

    recorder.onstop = () => {
      console.log(`MediaRecorder stopped. Total chunks: ${recordedChunksRef.current.length}. Final state: ${mediaRecorderRef.current?.state}`);
      // Clean up listener added in stopRecording
      mediaRecorderRef.current?.removeEventListener('dataavailable', (e: BlobEvent) => {}); // No-op remove just in case
      
      // Process data if chunks were collected
      if (recordedChunksRef.current.length > 0) {
        console.log("Processing recorded data...");
        setRecordingInProgress(true); // Show processing overlay NOW
        // Use timeout to allow state update before potentially heavy processing
        setTimeout(processRecording, 50); 
      } else {
        console.warn("Recording stopped, but no data chunks were collected.");
        // Only show error if recording wasn't immediately stopped (e.g. user error)
        if (recordingTime > 0) { 
             setCameraError("No video data was captured. Please try recording again.");
        }
         setRecordingInProgress(false); // Ensure no processing state lingers
      }
      // Detach recorder ref once stopped? Maybe not necessary, allows inspection.
      // mediaRecorderRef.current = null; 
    };

    recorder.onerror = (event: Event) => { // Use generic Event type
      const errorEvent = event as MediaRecorderErrorEvent; // Try casting for more info
      console.error("MediaRecorder error event:", errorEvent);
      let errorMsg = "An unknown recording error occurred.";
      if (errorEvent.error) {
          errorMsg = `Recording error: ${errorEvent.error.name} - ${errorEvent.error.message}`;
      }
      setCameraError(errorMsg);
      setIsRecording(false);
      setIsPaused(false);
      setRecordingInProgress(false);
      if (timerRef.current !== null) clearInterval(timerRef.current);
      timerRef.current = null;
       // Try stopping cleanly if possible, might prevent dangling resources
       try {
           if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
               mediaRecorderRef.current.stop();
           }
       } catch (stopErr) {
           console.warn("Error trying to stop recorder after error:", stopErr);
       }
    };

    // Start recording & UI updates
    recorder.start(500); // Collect chunks every 500ms - balances responsiveness and overhead
    console.log("MediaRecorder started. State:", recorder.state);

    setIsRecording(true);
    setRecordingTime(0);

    // Start timer
    timerRef.current = window.setInterval(() => {
      setRecordingTime(prev => {
        const newTime = prev + 1;
        if (newTime >= config.maxRecordingDuration) {
          console.log("Max recording duration reached. Stopping automatically.");
          stopRecording(); // Stop recording
          return config.maxRecordingDuration; // Cap display time
        }
        return newTime;
      });
    }, 1000);

  } catch (error: any) {
    console.error('Error in startRecording function:', error);
    setCameraError(`Failed to start recording: ${error.message}`);
    setIsRecording(false);
    setRecordingInProgress(false);
     if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
       try { mediaRecorderRef.current.stop(); } catch(e){}
     }
     mediaRecorderRef.current = null;
  }
}, [
    cameraReady, 
    isRecording, 
    recordingInProgress, 
    inputValues, 
    config, 
    processRecording, 
    stopRecording, 
    videoQuality,
    videoCodec, // Added to dependency array
    setInputValues, 
    initializeCamera, 
    setCameraError
  ]);

  // Force retry with screenshot if processing failed and user clicks button
  const retryAsScreenshot = useCallback(() => {
    if (lastRecordedBlobRef.current) {
      console.log("Manually triggering screenshot fallback from error alert.");
      setCameraError(null); // Clear the error message
      setRecordingInProgress(true); // Show processing indicator for screenshot attempt
      extractScreenshotFromVideo(lastRecordedBlobRef.current);
    } else {
      setCameraError("No previous video data available to create a screenshot.");
    }
  }, [extractScreenshotFromVideo]); // extractScreenshotFromVideo is stable

  // Format time for display
  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.round(seconds % 60); // Round seconds for display
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // Render Logic
  return (
    <Box sx={{ width: '100%', maxWidth: 600, mx: 'auto' }}>
      {/* Error Alert */}
      {cameraError && (
        <Alert 
          severity="error" 
          sx={{ mb: 2 }}
          onClose={cameraError.includes("Permission Denied") || cameraError.includes("Not Found") ? undefined : () => setCameraError(null)} // Allow dismissing recoverable errors
          action={
            // Show retry button only if it was a backend processing error AND we have the blob
            (outputValues?.errorMessage?.includes("No image or video data received") || cameraError.includes("Processing timed out") || cameraError.includes("Error encoding video")) && lastRecordedBlobRef.current ? (
              <Button 
                color="inherit" 
                size="small"
                onClick={retryAsScreenshot}
                startIcon={<RefreshIcon />}
                disabled={recordingInProgress} // Disable if retry is in progress
              >
                Retry as Photo
              </Button>
            ) : null
          }
        >
          {cameraError}
           {/* Add suggestion for permission errors */}
           {(cameraError.includes("Permission Denied") || cameraError.includes("Not Found") || cameraError.includes("Hardware Error")) &&
             <Typography variant="caption" display="block" sx={{mt: 1}}>Please check your browser's site settings to allow camera/microphone access for this page and ensure no other app is using the device, then refresh.</Typography>
           }
        </Alert>
      )}
      
      {/* Settings Buttons */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 1, gap: 1 }}>
        <Button 
          startIcon={<TuneIcon />} 
          size="small"
          variant="outlined"
          color="secondary"
          onClick={() => setShowAdvancedSettings(prev => !prev)}
          disabled={isRecording || recordingInProgress}
        >
          {showAdvancedSettings ? "Hide Quality" : "Video Quality"}
        </Button>
        <Button 
          startIcon={<SettingsIcon />} 
          size="small"
           variant="outlined"
           color="secondary"
          onClick={() => setShowDeviceSelector(prev => !prev)}
          disabled={isRecording || recordingInProgress}
        >
          {showDeviceSelector ? "Hide Devices" : "Select Camera"} 
        </Button>
      </Box>
      
      {/* Advanced Video Settings */}
      {showAdvancedSettings && (
        <Paper sx={{ p: 2, mb: 2 }} elevation={1}>
          <Typography variant="subtitle2" gutterBottom> Video Settings </Typography>
          <Stack spacing={2}>
            <FormControl size="small" fullWidth>
              <InputLabel id="quality-label">Video Quality</InputLabel>
              <Select
                labelId="quality-label" value={videoQuality} label="Video Quality"
                onChange={handleQualityChange} disabled={isRecording || recordingInProgress}
              >
                <MenuItem value="low">Low (Fastest Upload)</MenuItem>
                <MenuItem value="medium">Medium</MenuItem>
                <MenuItem value="high">High (Best Resolution)</MenuItem>
              </Select>
            </FormControl>
            
            {/* Video Codec Selector */}
            <FormControl size="small" fullWidth>
              <InputLabel id="codec-label">Video Codec</InputLabel>
              <Select
                labelId="codec-label"
                value={videoCodec}
                label="Video Codec"
                onChange={(e) => setVideoCodec(e.target.value)}
                disabled={isRecording || recordingInProgress}
              >
                <MenuItem value="h264">
                  H.264 (MP4)
                  <Tooltip title="Most compatible codec, works on all platforms including iOS">
                    <InfoIcon fontSize="small" sx={{ ml: 1, verticalAlign: 'middle', opacity: 0.6 }} />
                  </Tooltip>
                </MenuItem>
                <MenuItem value="vp9">
                  VP9 (WebM)
                  <Tooltip title="Better compression, supported on most modern browsers except Safari">
                    <InfoIcon fontSize="small" sx={{ ml: 1, verticalAlign: 'middle', opacity: 0.6 }} />
                  </Tooltip>
                </MenuItem>
                <MenuItem value="vp8">
                  VP8 (WebM)
                  <Tooltip title="Older WebM codec, widely supported except on older iOS">
                    <InfoIcon fontSize="small" sx={{ ml: 1, verticalAlign: 'middle', opacity: 0.6 }} />
                  </Tooltip>
                </MenuItem>
              </Select>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
                H.264 recommended for best compatibility
              </Typography>
            </FormControl>
            
            <Box>
              <Typography variant="body2" gutterBottom> Max Recording: {maxDuration} sec </Typography>
              <Slider
                min={10} max={60} step={10} // Changed from 5-15 to 10-60 seconds
                marks={[
                  { value: 10, label: '10s' }, 
                  { value: 20, label: '20s' },
                  { value: 30, label: '30s' }, 
                  { value: 60, label: '60s' }
                ]}
                value={maxDuration}
                onChange={(_, newValue) => setMaxDuration(newValue as number)}
                disabled={isRecording || recordingInProgress}
                sx={{ mt: 1 }}
              />
              <Typography variant="caption" color="text.secondary">
                Longer videos may take more time to process
              </Typography>
            </Box>
          </Stack>
        </Paper>
      )}
      
      {/* Device Selection UI */}
      {showDeviceSelector && (
        <Paper sx={{ p: 2, mb: 2 }} elevation={1}>
          <Typography variant="subtitle2" gutterBottom> Camera & Microphone </Typography>
          <Stack spacing={2}>
            <FormControl size="small" fullWidth>
              <InputLabel id="video-device-label">Camera</InputLabel>
              <Select
                labelId="video-device-label" value={selectedVideoDevice} label="Camera"
                onChange={handleVideoDeviceChange} disabled={isRecording || videoDevices.length <= 1 || recordingInProgress}
              >
                {videoDevices.map((device) => (<MenuItem key={device.deviceId} value={device.deviceId}>{device.label}</MenuItem>))}
                {videoDevices.length === 0 && (<MenuItem value="" disabled><em>No cameras found</em></MenuItem>)}
              </Select>
            </FormControl>
            <FormControl size="small" fullWidth>
              <InputLabel id="audio-device-label">Microphone</InputLabel>
              <Select
                labelId="audio-device-label" value={selectedAudioDevice} label="Microphone"
                onChange={handleAudioDeviceChange} disabled={isRecording || audioDevices.length === 0 || recordingInProgress}
              >
                 <MenuItem value=""><em>Default Microphone</em></MenuItem> {/* Allow selecting default */}
                {audioDevices.map((device) => (<MenuItem key={device.deviceId} value={device.deviceId}>{device.label}</MenuItem>))}
                 {/* Removed the "no microphones" message to allow default selection */}
              </Select>
            </FormControl>
          </Stack>
        </Paper>
      )}
      
      {/* Camera Preview Area */}
      <Paper 
        elevation={3}
        sx={{ 
          position: 'relative', mb: 2, p: 0, borderRadius: 2, overflow: 'hidden',
          backgroundColor: '#333', // Darker background for loading/error states
          aspectRatio: config.aspectRatio, maxHeight: config.maxHeight
        }}
      >
        {/* Video Element */}
        <video
          ref={videoRef} autoPlay playsInline muted
          style={{ 
             width: '100%', height: '100%', objectFit: 'cover', 
             display: cameraReady ? 'block' : 'none' // Hide if not ready
           }}
        />
        
        {/* Loading / Initializing / Error Display */}
        {(!cameraReady || !streamRef.current) && !isRecording && (
             <Box sx={{ /* Styling for loading/error overlay */
                position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
                display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                backgroundColor: 'rgba(0, 0, 0, 0.7)', color: 'white', p: 2, textAlign: 'center', zIndex: 5
             }}>
                 {loading && !cameraError && <CircularProgress color="inherit" size={40} sx={{ mb: 2 }} />}
                 <Typography variant="body1">
                    {cameraError ? "Camera Error" : "Initializing Camera..."}
                 </Typography>
                 {cameraError && <Typography variant="caption" sx={{mt: 1}}>{cameraError.split(':')[1] || cameraError}</Typography>}
                 {!cameraError && <Typography variant="caption" sx={{mt: 1}}>Please allow camera/microphone access if prompted.</Typography>}
             </Box>
        )}

         {/* Processing Overlay */}
        {recordingInProgress && (
             <Box sx={{ /* Styling identical to loading overlay */
                position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
                display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                backgroundColor: 'rgba(0, 0, 0, 0.8)', color: 'white', p: 2, textAlign: 'center', zIndex: 15 // Higher z-index
             }}>
                <CircularProgress color="primary" size={48} sx={{ mb: 2 }} />
                <Typography variant="body1"> Processing {lastRecordedBlobRef.current ? "Video" : "Capture"}... </Typography>
                <Typography variant="caption" sx={{ mt: 1, maxWidth: '80%' }}> 
                   {lastRecordedBlobRef.current && lastRecordedBlobRef.current?.size > 1024*1024 ? "This may take a moment..." : "Please wait..."}
                </Typography>
             </Box>
         )}
        
        {/* Recording Indicator (only when actually recording) */}
        {isRecording && !isPaused && (
          <Box sx={{ 
              position: 'absolute', top: 10, right: 10, backgroundColor: 'rgba(0, 0, 0, 0.6)',
              color: 'error.main', p: '4px 8px', borderRadius: 1, display: 'flex', alignItems: 'center', gap: 1, zIndex: 10 
          }}>
            <Box sx={{ width: 10, height: 10, borderRadius: '50%', backgroundColor: 'error.main', animation: 'pulse 1.5s infinite' }} />
            <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'white', lineHeight: 1 }}> REC {formatTime(recordingTime)} </Typography>
          </Box>
        )}
         {/* Paused Indicator */}
         {isRecording && isPaused && (
             <Box sx={{ position: 'absolute', top: 10, right: 10, backgroundColor: 'rgba(0, 0, 0, 0.6)', 
                 color: 'warning.main', p: '4px 8px', borderRadius: 1, display: 'flex', alignItems: 'center', gap: 1, zIndex: 10 }}>
                 <PauseIcon fontSize="small" sx={{ color: 'warning.main' }} />
                 <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'white', lineHeight: 1 }}> PAUSED {formatTime(recordingTime)} </Typography>
             </Box>
         )}
        
        {/* Floating control buttons during recording */}
        {isRecording && (
          <Box sx={{ position: 'absolute', bottom: 16, left: '50%', transform: 'translateX(-50%)', display: 'flex', gap: 2, zIndex: 20 }}>
            <Fab color={isPaused ? "secondary" : "warning"} size="medium" onClick={togglePause} disabled={recordingInProgress} aria-label={isPaused ? "Resume recording" : "Pause recording"}>
              {isPaused ? <PlayArrowIcon /> : <PauseIcon />}
            </Fab>
            <Fab color="error" size="medium" onClick={stopRecording} disabled={recordingInProgress} aria-label="Stop recording">
              <StopIcon />
            </Fab>
          </Box>
        )}
      </Paper>
      
      {/* Optional filename input */}
      {config.showFilenameInput && config.fileNameFieldName && (
        <TextField fullWidth label="Filename (Optional)" placeholder="my_capture" variant="outlined" margin="dense" // smaller margin
          value={inputValues[config.fileNameFieldName] || ''}
          onChange={(e) => setInputValues({ ...inputValues, [config.fileNameFieldName]: e.target.value })}
          disabled={loading || isRecording || recordingInProgress} sx={{ mb: 2 }}
        />
      )}
      
      {/* Action buttons area */}
      <Box sx={{ mb: 3, mt: isRecording ? 2 : 0 }}>
         {/* Buttons when NOT recording */}
        {!isRecording ? (
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="center">
            <Button variant="contained" color="primary" id="button-takePhotoButton" startIcon={<CameraAltIcon />}
                onClick={capturePhoto} disabled={!cameraReady || loading || recordingInProgress} fullWidth sx={{py: 1.5}}>
                Take Photo
            </Button>
            <Button variant="contained" color="secondary" id="button-startRecordButton" startIcon={<VideocamIcon />}
                onClick={startRecording} disabled={!cameraReady || loading || recordingInProgress} fullWidth sx={{py: 1.5}}>
                Record Video ({maxDuration}s)
            </Button>
            </Stack>
        ) : (
            /* Controls shown INSTEAD of floating buttons when recording, for accessibility */
            <Box>
                 <Typography variant="body2" align="center" sx={{ mb: 1.5, fontStyle: 'italic' }}>
                    {isPaused 
                    ? `Paused at ${formatTime(recordingTime)} / ${formatTime(config.maxRecordingDuration)}` 
                    : `Recording: ${formatTime(recordingTime)} / ${formatTime(config.maxRecordingDuration)}`}
                 </Typography>
                 {/* Non-floating controls (duplicate for accessibility) */}
                 <Grid container spacing={2}>
                     <Grid item xs={6}>
                     <Button variant="outlined" fullWidth startIcon={isPaused ? <PlayArrowIcon /> : <PauseIcon />}
                         onClick={togglePause} color={isPaused ? "secondary" : "warning"} disabled={recordingInProgress} sx={{py: 1}}>
                         {isPaused ? "Resume" : "Pause"}
                     </Button>
                     </Grid>
                     <Grid item xs={6}>
                     <Button variant="contained" fullWidth color="error" id="button-stopRecordButton-alt" startIcon={<StopIcon />}
                         onClick={stopRecording} disabled={recordingInProgress} sx={{py: 1}}>
                         Stop & Save
                     </Button>
                     </Grid>
                 </Grid>
            </Box>
        )}
      </Box>
      
      {/* Results Display with Download Button */}
      {outputValues.success && (
        <Alert severity="success" sx={{ mb: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <Box>
            <Typography variant="body1" gutterBottom> {outputValues.statusMessage || "Capture saved successfully"} </Typography>
              {outputValues.savedFileName && <Typography variant="body2"> Name: <strong>{outputValues.savedFileName}</strong> </Typography>}
              {outputValues.savedFileSize && <Typography variant="body2"> Size: <strong>{outputValues.savedFileSize}</strong> </Typography>}
              {outputValues.videoCodec && <Typography variant="body2"> Video Codec: <strong>{outputValues.videoCodec}</strong> </Typography>}
            </Box>
            <Button 
              variant="outlined" 
              size="small" 
              color="success" 
              onClick={handleDownload}
              startIcon={<GetAppIcon />}
              sx={{ ml: 2, minWidth: '120px' }}
            >
              Download
            </Button>
          </Box>
        </Alert>
      )}
      
  {/* Image Preview on Success */}
  {outputValues.success && outputValues.savedFileType === 'image' && outputValues.capturedImagePreview && (
    <Box sx={{ mt: 2, textAlign: 'center', border: '1px solid', borderColor: 'divider', p: 1, borderRadius: 1 }}>
      <Typography variant="subtitle2" gutterBottom> Saved Photo Preview: </Typography>
      <img 
        src={outputValues.capturedImagePreview.startsWith('data:') 
          ? outputValues.capturedImagePreview 
          : `data:image/jpeg;base64,${outputValues.capturedImagePreview}`} 
        alt="Saved Capture" 
        style={{ maxWidth: '80%', maxHeight: 200, borderRadius: 4 }}
      />
      <Button
        variant="outlined"
        size="small"
        color="primary"
        startIcon={<GetAppIcon />}
        onClick={(e) => {
          e.stopPropagation(); // Prevent event bubbling
          // Direct download implementation
          try {
            const imageData = outputValues.capturedImagePreview.startsWith('data:')
              ? outputValues.capturedImagePreview
              : `data:image/jpeg;base64,${outputValues.capturedImagePreview}`;
              
            const filename = outputValues.savedFileName || 'captured_image.jpg';
            const link = document.createElement('a');
            link.href = imageData;
            link.download = filename;
            link.target = '_blank'; // Open in new tab
            document.body.appendChild(link);
            link.click();
            setTimeout(() => document.body.removeChild(link), 100);
          } catch (err) {
            console.error("Error downloading image:", err);
          }
        }}
        sx={{ mt: 1 }}
      >
        Save Image
      </Button>
    </Box>
  )}
      
      {/* General Loading Indicator (e.g., during parent component submission) */}
      {loading && !recordingInProgress && ( // Show only if parent is loading but component isn't processing video
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', my: 2, color: 'text.secondary' }}>
          <CircularProgress size={20} sx={{ mr: 1 }} color="inherit" />
          <Typography variant="body2"> Submitting... </Typography>
        </Box>
      )}
      
      {/* Pulse Animation CSS */}
      <style dangerouslySetInnerHTML={{ __html: `
        @keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.4; } 100% { opacity: 1; } }
      `}} />
    </Box>
  );
});

export default WebCamCapture;