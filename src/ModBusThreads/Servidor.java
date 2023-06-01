/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ModBusThreads;

import de.re.easymodbus.server.ModbusServer;
import java.io.IOException;
import static java.lang.System.exit;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Usuario El servidor es el esclavo
 *
 */
public class Servidor extends Thread {

    ModbusServer Servidor;
    public boolean FlgRead = false, FlgWrite = false, FlgConnect = false, FlgDisconnect = true;
    public int StartAddres, quantity = 65535;
    public boolean RCoils[], WCoils[];
    public boolean RDiscreteInputs[];
    public int RHoldingRegisters[], WHoldingRegisters[];
    public int RInputRegisters[];
    int nTmrRetry = 5000, nRetry = 2, nActualRetry, nTmrScan = 1000;
    public boolean bDebugMode;
    public String strFault=" No Faults", strStatus="Disconnected";
    public int RegisterType = 0;
    public int nNumberofclientsconnected = 0;
    private int PID;

    public Servidor(String ip, String Port, String id, String StrtAddres, String strQuantity, boolean bDebug) {
        this.Servidor = new ModbusServer();
        this.Servidor.setPort(Integer.parseInt(Port));
        this.StartAddres = Integer.parseInt(StrtAddres);
        this.nNumberofclientsconnected = 0;
        //this.quantity = Integer.parseInt(strQuantity);
        this.nActualRetry = 0;
        this.bDebugMode = bDebug;
        setDaemon(true);
        DisconnectFlg();
        Status();
        initialice_arrays();
      
    }

    public Servidor() {
        this.Servidor = new ModbusServer();
        this.nActualRetry = 0;
        DisconnectFlg();
        Status();
        initialice_arrays();
        setDaemon(true);
     

    }

    public void setParams(String ip, String Port, String id, String StrtAddres, String strQuantity, boolean bDebug) throws IOException {

        this.Servidor.setPort(Integer.parseInt(Port));

        this.StartAddres = Integer.parseInt(StrtAddres) + 1;
        //this.quantity = Integer.parseInt(strQuantity);
        this.nActualRetry = 0;
        this.bDebugMode = bDebug;
        DisconnectFlg();
        Status();
        initialice_arrays();
       

    }

    private void check_Retry() {
        if (this.nActualRetry >= this.nRetry) {
            this.Servidor.StopListening();
            this.nActualRetry = 0;
            DisconnectFlg();
        } else {
            try {
                this.nActualRetry++;
                Thread.sleep(this.nTmrRetry);
            } catch (InterruptedException ex) {
                this.strFault = ex.getMessage();
                fdebug(this.strFault, this.bDebugMode);
            }
        }

    }

    private void fdebug(String str, boolean active) {
        if (active) {
            System.out.println(str);
        }
    }

    private void Status() {
        if (this.FlgConnect) {
            this.strStatus = "Server Connecting";
            if (this.FlgConnect && this.Servidor.getServerRunning()) {
                //FlgRead = false, FlgWrite = false, FlgConnect = false, FlgDisconnect = false;
                this.strStatus = "Server Connected";
                if (this.FlgRead) {
                    this.strStatus = "Server Reading";
                }
                if (this.FlgWrite) {
                    this.strStatus = "Server Reading and Writing";
                }

            }

        }
        if (this.FlgDisconnect) {
            this.strStatus = "Server Disconnecting";
            if (this.FlgDisconnect && this.Servidor.getServerRunning() == false) {
                this.strStatus = "Server Disconected";

            }
        }
        fdebug(this.strStatus, this.bDebugMode);
    }

    public void ConnectFlg() {
        this.FlgDisconnect = false;
        this.FlgConnect = true;
    }

    public void DisconnectFlg() {
        this.FlgRead = false;
        this.FlgWrite = false;
        this.FlgConnect = false;
        this.FlgDisconnect = true;

    }

    private void WaitFor(boolean signal, boolean condition) throws InterruptedException {
        do {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (signal != condition);
    }

    private void WaitFor(String str, boolean signal, boolean condition) throws InterruptedException {
        do {
            try {
                fdebug(str, this.bDebugMode);
                Thread.sleep(2000);

            } catch (InterruptedException ex) {
                Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (signal != condition);
    }

    private void initialice_arrays() {
        this.RCoils = new boolean[this.quantity];
        this.RDiscreteInputs = new boolean[this.quantity];
        this.RHoldingRegisters = new int[this.quantity];
        this.RInputRegisters = new int[this.quantity];
        this.WCoils = new boolean[this.quantity];
        this.WHoldingRegisters = new int[this.quantity];
        for (int i = 0; i < this.quantity; i++) {
            this.RCoils[i] = false;
            this.RDiscreteInputs[i] = false;
            this.RHoldingRegisters[i] = 0;
            this.RInputRegisters[i] = 0;
            this.WCoils[i] = false;
            this.WHoldingRegisters[i] = 0;
        }

    }

    private void getPID() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        String jvmName = runtimeBean.getName();
        this.PID = Integer.parseInt(jvmName.split("@")[0]);
        fdebug("El PID del server es:" + this.PID, this.bDebugMode);
    }

    private void KillPID(int pid) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("taskkill " + pid);
        int ExitCode = process.waitFor();
        if (ExitCode == 0) {
            System.out.println("El proceso con PID " + pid + " ha sido detenido.");
        } else {
            System.err.println("El proceso con PID " + pid + " no se ha podido detener.");
        }
    }

    //**********************************runable******************
    public void run() {
        do {
            try {

                if ((this.FlgConnect == true) && (this.Servidor.getServerRunning() == false)) {
                    try {
                        if (Servidor.isAlive()) {
                            this.Servidor.StopListening();
                        }
                        this.Servidor.Listen();

                    } catch (IOException ex) {

                        this.strFault = ex.getMessage();
                        fdebug(this.strFault, this.bDebugMode);
                    }

                } else if ((this.FlgConnect == false) && (this.Servidor.getServerRunning() == false)) {
                    this.strStatus="Waiting for Connect";
                    fdebug( this.strStatus, this.bDebugMode);
                }

                if (this.FlgRead) {
                    switch (RegisterType) {
                        case 0:// coils

                            this.RCoils = this.Servidor.coils;
                            break;
                        case 1://discrete inputs

                            this.RDiscreteInputs = this.Servidor.discreteInputs;
                            break;
                        case 2://holding registers

                            this.RHoldingRegisters = this.Servidor.holdingRegisters;
                            break;
                        case 3://input registers

                            this.RInputRegisters = this.Servidor.inputRegisters;
                            break;
                    }
                }// read flag
                if (this.FlgWrite) {
                    switch (RegisterType) {
                        case 0://coils
                            //this.Servidor.WriteMultipleCoils(this.StartAddres, this.WCoils);

                            break;
                        case 1:// discrete inputs
                            break;
                        case 2://holding registers
                            //this.Servidor.WriteMultipleRegisters(this.StartAddres, this.WHoldingRegisters);
                            this.Servidor.holdingRegisters[0] = this.WHoldingRegisters[0];
                            this.Servidor.holdingRegisters[1] = this.WHoldingRegisters[1];
                            this.Servidor.holdingRegisters[2] = this.WHoldingRegisters[2];
                            this.Servidor.holdingRegisters[3] = this.WHoldingRegisters[3];
                            this.Servidor.holdingRegisters[4] = this.WHoldingRegisters[4];
                            this.Servidor.holdingRegisters[5] = this.WHoldingRegisters[5];
                            this.Servidor.holdingRegisters[6] = this.WHoldingRegisters[6];
                            this.Servidor.holdingRegisters[7] = this.WHoldingRegisters[7];
                            this.Servidor.holdingRegisters[8] = this.WHoldingRegisters[8];
                            this.Servidor.holdingRegisters[9] = this.WHoldingRegisters[9];

                            break;
                        case 3://input registers
                            break;
                    }

                }//write flag

                if (this.FlgDisconnect) {
                    this.Servidor.StopListening();
                    //getPID();
                    //KillPID(this.PID);

                }// diconnect flag
              
                this.nNumberofclientsconnected = this.Servidor.getNumberOfConnectedClients();
                Status();

                Thread.sleep(this.nTmrScan);

            } catch (InterruptedException ex) {
                this.strFault = ex.getMessage();
                fdebug(this.strFault, this.bDebugMode);
                check_Retry();
            }

        } while (true);
    }
}
