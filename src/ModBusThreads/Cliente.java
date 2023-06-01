/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ModBusThreads;

import de.re.easymodbus.exceptions.ModbusException;
import de.re.easymodbus.modbusclient.ModbusClient;
import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

/**
 *
 * @author Usuario El el cliente es el master
 */
public class Cliente extends Thread {

    ModbusClient Cliente;
    public boolean FlgRead = false, FlgWrite = false, FlgConnect = false, FlgDisconnect = true;
    public int StartAddres, quantity = 10;
    public boolean RCoils[], WCoils[];
    public boolean RDiscreteInputs[];
    public int RHoldingRegisters[], WHoldingRegisters[];
    public int RInputRegisters[];
    int nTmrRetry = 5000, nRetry = 2, nActualRetry, nTmrScan = 1000;
    public boolean bDebugMode;
    public String strFault=" No Faults", strStatus="Disconnected";
    public int RegisterType = 0;

    public Cliente(String ip, String Port, String id, String StrtAddres, String strQuantity, boolean bDebug) {
        this.Cliente = new ModbusClient();
        this.Cliente.setipAddress(ip);
        this.Cliente.setPort(Integer.parseInt(Port));
        this.Cliente.setUnitIdentifier(Byte.parseByte(id));
        this.StartAddres = Integer.parseInt(StrtAddres);
        //this.quantity = Integer.parseInt(strQuantity);
        this.nActualRetry = 0;
        this.bDebugMode = bDebug;
        DisconnectFlg();
        Status();
        initialice_arrays();
       
    }

    public Cliente() {
        this.Cliente = new ModbusClient();
       
        this.nActualRetry = 0;
        DisconnectFlg();
        Status();
        initialice_arrays();
    }

    public void setParams(String ip, String Port, String id, String StrtAddres, String strQuantity, boolean bDebug) {
        this.Cliente.setipAddress(ip);
        this.Cliente.setPort(Integer.parseInt(Port));
        this.Cliente.setUnitIdentifier(Byte.parseByte(id));
        this.StartAddres = Integer.parseInt(StrtAddres);
        //this.quantity = Integer.parseInt(strQuantity);
        this.nActualRetry = 0;
        this.bDebugMode = bDebug;
        DisconnectFlg();
        Status();
        initialice_arrays();
      
    }

    public void run() {
        do {
            try {

                if ((this.FlgConnect == true) && (this.Cliente.isConnected() == false)) {
                    try {

                        this.Cliente.Connect();
                    } catch (IOException ex) {

                        this.strFault = ex.getMessage();
                        fdebug(this.strFault, this.bDebugMode);
                    }

                } else if ((this.FlgConnect == false) && (this.Cliente.isConnected() == false)) {
                    this.strStatus="Waiting For Connect";
                    fdebug("esperando Flag conectar", this.bDebugMode);

                }

                if (this.FlgRead) {
                    try {
                        switch (RegisterType) {
                            case 0:// coils
                                this.RCoils = this.Cliente.ReadCoils(this.StartAddres, this.quantity);
                                break;
                            case 1://discrete inputs
                                this.RDiscreteInputs = this.Cliente.ReadDiscreteInputs(this.StartAddres, this.quantity);
                                break;
                            case 2://holding registers

                                this.RHoldingRegisters = this.Cliente.ReadHoldingRegisters(this.StartAddres, this.quantity);
                                break;
                            case 3://input registers

                                this.RInputRegisters = this.Cliente.ReadInputRegisters(this.StartAddres, this.quantity);
                                break;
                        }
                    } catch (ModbusException ex) {

                        this.strFault = ex.getMessage();
                        fdebug(this.strFault, this.bDebugMode);
                        check_Retry();

                    } catch (SocketException ex) {
                        this.strFault = ex.getMessage();
                        fdebug(this.strFault, this.bDebugMode);
                        check_Retry();

                    } catch (IOException ex) {
                        this.strFault = ex.getMessage();
                        fdebug(this.strFault, this.bDebugMode);
                        check_Retry();
                    }
                }// read flag
                if (this.FlgWrite) {
                    switch (RegisterType) {
                        case 0://coils
                            this.Cliente.WriteMultipleCoils(this.StartAddres, this.WCoils);
                            break;
                        case 1:// discrete inputs
                            break;
                        case 2://holding registers
                            this.Cliente.WriteMultipleRegisters(this.StartAddres, this.WHoldingRegisters);

                            break;
                        case 3://input registers
                            break;
                    }

                }//write flag

                if (this.FlgDisconnect) {
                    this.Cliente.Disconnect();
                }// diconnect flag

                Status();
                Thread.sleep(this.nTmrScan);

            } catch (InterruptedException ex) {
                this.strFault = ex.getMessage();
                fdebug(this.strFault, this.bDebugMode);
                check_Retry();
            } catch (IOException ex) {
                this.strFault = ex.getMessage();
                fdebug(this.strFault, this.bDebugMode);
            } catch (ModbusException ex) {
                 this.strFault = ex.getMessage();
                Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
            }

        } while (true);
    }

    private void check_Retry() {
        if (this.nActualRetry >= this.nRetry) {
            try {
                this.Cliente.Disconnect();
                this.nActualRetry = 0;
                DisconnectFlg();
            } catch (IOException ex) {
                this.strFault = ex.getMessage();
                fdebug(this.strFault, this.bDebugMode);
            }
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

    public void fdebug(String str,boolean active,JTextArea text) {
        if (active) {
            System.out.println(str);
            text.setText(text.getText()+str+"\n");

        }
     
    }

    private void Status() {
        if (this.FlgConnect) {
            this.strStatus = "Connecting";
            if (this.FlgConnect && this.Cliente.isConnected()) {
                //FlgRead = false, FlgWrite = false, FlgConnect = false, FlgDisconnect = false;
                this.strStatus = "Connected";
                if (this.FlgRead) {
                    this.strStatus = "Reading";
                }
                if (this.FlgWrite) {
                    this.strStatus = "Reading and Writing";
                }

            }

        }
        if (this.FlgDisconnect) {
            this.strStatus = "Disconnecting";
            if (this.FlgDisconnect && this.Cliente.isConnected() == false) {
                this.strStatus = "Disconected";

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
}
