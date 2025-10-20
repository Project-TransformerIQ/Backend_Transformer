"""
Mock Classification Training Server
This is a simple Flask server that simulates the classification training endpoint
for testing purposes. It receives the training data and returns an updated configuration.

Usage:
    python mock_classification_server.py

The server will run on http://localhost:5000
NOTE: This is for testing only. In production, use the actual Flask API at port 5000.
"""

from flask import Flask, request, jsonify
import random
import json
from flask_cors import CORS

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

@app.route('/update-config', methods=['POST'])
def update_config():
    """
    Mock training endpoint that receives:
    - baseline_image (file)
    - maintenance_image (file)
    - config (JSON file)
    - anomaly_results (JSON file)
    
    Returns an updated configuration with slightly modified parameters
    """
    print("\n" + "="*60)
    print("TRAINING REQUEST RECEIVED")
    print("="*60)
    
    # Check received files
    baseline_image = request.files.get('baseline_image')
    maintenance_image = request.files.get('maintenance_image')
    config_file = request.files.get('config')
    anomaly_file = request.files.get('anomaly_results')
    
    print(f"\nReceived files:")
    print(f"  - baseline_image: {baseline_image.filename if baseline_image else 'NOT FOUND'}")
    print(f"  - maintenance_image: {maintenance_image.filename if maintenance_image else 'NOT FOUND'}")
    print(f"  - config: {config_file.filename if config_file else 'NOT FOUND'}")
    print(f"  - anomaly_results: {anomaly_file.filename if anomaly_file else 'NOT FOUND'}")
    
    # Validate all required files are present
    if not all([baseline_image, maintenance_image, config_file, anomaly_file]):
        missing = []
        if not baseline_image: missing.append('baseline_image')
        if not maintenance_image: missing.append('maintenance_image')
        if not config_file: missing.append('config')
        if not anomaly_file: missing.append('anomaly_results')
        
        return jsonify({
            "status": "error",
            "message": f"Missing required files: {', '.join(missing)}"
        }), 400
    
    # Parse the configuration
    try:
        config_data = json.loads(config_file.read().decode('utf-8'))
        print(f"\nCurrent Configuration:")
        print(json.dumps(config_data, indent=2))
    except Exception as e:
        return jsonify({
            "status": "error",
            "message": f"Failed to parse config file: {str(e)}"
        }), 400
    
    # Parse anomaly results
    try:
        anomaly_data = json.loads(anomaly_file.read().decode('utf-8'))
        fault_count = len(anomaly_data.get('fault_regions', []))
        print(f"\nAnomaly Results:")
        print(f"  - Fault regions detected: {fault_count}")
    except Exception as e:
        return jsonify({
            "status": "error",
            "message": f"Failed to parse anomaly results: {str(e)}"
        }), 400
    
    # Simulate training process
    print("\n" + "-"*60)
    print("SIMULATING TRAINING PROCESS...")
    print("-"*60)
    print("  1. Loading baseline image...")
    print("  2. Loading maintenance image...")
    print("  3. Analyzing fault regions...")
    print("  4. Training classification model...")
    print("  5. Optimizing configuration parameters...")
    print("  6. Generating updated configuration...")
    
    # Generate updated configuration
    # Slightly adjust parameters based on "training"
    updated_config = {
        "ssim": {
            "weight": round(config_data.get('ssim', {}).get('weight', 0.5) + random.uniform(-0.1, 0.1), 3),
            "threshold": round(config_data.get('ssim', {}).get('threshold', 0.85) + random.uniform(-0.05, 0.05), 3)
        },
        "mse": {
            "weight": round(config_data.get('mse', {}).get('weight', 0.3) + random.uniform(-0.05, 0.05), 3),
            "threshold": round(config_data.get('mse', {}).get('threshold', 1000.0) + random.uniform(-100, 100), 1)
        },
        "histogram": {
            "weight": round(config_data.get('histogram', {}).get('weight', 0.2) + random.uniform(-0.05, 0.05), 3),
            "threshold": round(config_data.get('histogram', {}).get('threshold', 0.7) + random.uniform(-0.05, 0.05), 3)
        },
        "combined_threshold": round(config_data.get('combined_threshold', 0.75) + random.uniform(-0.05, 0.05), 3),
        "image_processing": {
            "resize_width": config_data.get('image_processing', {}).get('resize_width', 800),
            "resize_height": config_data.get('image_processing', {}).get('resize_height', 600),
            "blur_kernel_size": random.choice([3, 5, 7])
        },
        "detection": {
            "min_contour_area": max(50, config_data.get('detection', {}).get('min_contour_area', 100) + random.randint(-20, 20)),
            "dilation_iterations": random.randint(1, 4),
            "erosion_iterations": random.randint(1, 3)
        }
    }
    
    # Ensure weights sum to approximately 1.0
    total_weight = (updated_config['ssim']['weight'] + 
                   updated_config['mse']['weight'] + 
                   updated_config['histogram']['weight'])
    
    if total_weight > 0:
        updated_config['ssim']['weight'] = round(updated_config['ssim']['weight'] / total_weight, 3)
        updated_config['mse']['weight'] = round(updated_config['mse']['weight'] / total_weight, 3)
        updated_config['histogram']['weight'] = round(updated_config['histogram']['weight'] / total_weight, 3)
    
    print("\n" + "-"*60)
    print("TRAINING COMPLETED")
    print("-"*60)
    print(f"\nUpdated Configuration:")
    print(json.dumps(updated_config, indent=2))
    
    response = {
        "status": "success",
        "message": f"Model trained successfully. Analyzed {fault_count} fault regions and optimized configuration parameters.",
        "updated_config": updated_config,
        "training_metrics": {
            "fault_regions_analyzed": fault_count,
            "baseline_image_size": baseline_image.content_length if baseline_image else 0,
            "maintenance_image_size": maintenance_image.content_length if maintenance_image else 0,
            "training_duration_ms": random.randint(1000, 5000)
        }
    }
    
    print("\n" + "="*60)
    print("RESPONSE SENT")
    print("="*60 + "\n")
    
    return jsonify(response), 200

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({
        "status": "healthy",
        "service": "Mock Classification Training Server",
        "version": "1.0.0"
    }), 200

@app.route('/', methods=['GET'])
def index():
    """Root endpoint with server info"""
    return jsonify({
        "service": "Mock Classification Training Server",
        "version": "1.0.0",
        "endpoints": {
            "/update-config": "POST - Train classification model and update config",
            "/detect-anomalies": "POST - Detect anomalies (not implemented in mock)",
            "/health": "GET - Health check"
        },
        "status": "running"
    }), 200

if __name__ == '__main__':
    print("\n" + "="*60)
    print("Mock Classification Training Server")
    print("="*60)
    print("\nStarting server on http://localhost:5000")
    print("\nAvailable endpoints:")
    print("  POST /update-config  - Train classification model and update config")
    print("  GET  /health         - Health check")
    print("\nNOTE: This runs on port 5000, same as the actual Flask API")
    print("      Stop the actual Flask API before running this mock server")
    print("\nPress Ctrl+C to stop the server")
    print("="*60 + "\n")
    
    app.run(host='0.0.0.0', port=5000, debug=True)
