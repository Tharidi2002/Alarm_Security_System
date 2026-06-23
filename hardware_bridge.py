# import serial
# import requests
# import json
# import time


# SERIAL_PORT = 'COM3'  
# BAUD_RATE = 115200
# API_URL = 'http://192.168.8.116:8080/api/alerts/sms-simulate'
# SIM_NUMBER = '0771234567' 

# print(f"[*] Starting Full-Stack Hardware Bridge on {SERIAL_PORT}...")

# try:
#     ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
#     time.sleep(2) 
#     print("[+] Bridge Active. Listening for Z8B Security Incidents...\n")
    
#     while True:
#         if ser.in_available() or ser.in_waiting > 0:
#             line = ser.readline().decode('utf-8', errors='ignore').strip()
#             if line:
#                 print(f"[RAW HARDWARE]: {line}") 
                
#                 payload_msg = ""
#                 if "[STATUS]: SYSTEM FAULT / WARNING DETECTED" in line:
#                     payload_msg = "Z8B Alert: Host Low Battery or AC Power Failure"
#                 elif "[STATUS]: !!! CRITICAL SECURITY BREACH !!!" in line:
#                     payload_msg = "Z8B CRITICAL: WIRE CUT / SENSOR INTRUSION DETECTED AT ATM!"
                
#                 if payload_msg:
#                     print(f"\n[!] MATCH FOUND! Forwarding to Spring Boot API...")
#                     data_to_send = {
#                         "simNumber": SIM_NUMBER,
#                         "message": payload_msg
#                     }
#                     try:
#                         response = requests.post(API_URL, json=data_to_send)
#                         print(f"[Backend Response]: Status Code {response.status_code} - Success!\n")
#                     except Exception as e:
#                         print(f"[-] Failed to connect to Spring Boot: {e}\n")
                        
# except serial.SerialException:
#     print(f"[-] Error: Could not open port {SERIAL_PORT}. Please check connection or COM port number.")
# except KeyboardInterrupt:
#     print("\n[-] Exiting Hardware Bridge.")