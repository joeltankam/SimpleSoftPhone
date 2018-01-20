package ma.ac.emi.simplesoftphone.ui;

import ma.ac.emi.simplesoftphone.sip.SipListenerServer;

import javax.sip.Dialog;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Date;

public class SipServer extends javax.swing.JFrame {

    /**
     * Creates new form SipRegistrar
     */
    public SipServer() {
        initComponents();
        new SipListenerServer(this);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPaneTable = new javax.swing.JScrollPane();
        jTable = new javax.swing.JTable();
        jScrollPaneText = new javax.swing.JScrollPane();
        jTextArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SIP Server");
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                onOpen(evt);
            }
        });

        jTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{

                },
                new String[]{
                        "Time", "URI", "From", "To", "Call-ID", "CSeq", "Dialog", "Transaction", "Type", "Request/Response"
                }
        ) {
            Class[] types = new Class[]{
                    java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[]{
                    false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPaneTable.setViewportView(jTable);

        jTextArea.setEditable(false);
        jTextArea.setColumns(20);
        jTextArea.setRows(5);
        jScrollPaneText.setViewportView(jTextArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPaneTable, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                        .addComponent(jScrollPaneText, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPaneTable, javax.swing.GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPaneText, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void onOpen(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_onOpen

    }//GEN-LAST:event_onOpen

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SipServer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SipServer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SipServer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SipServer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SipServer().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPaneTable;
    private javax.swing.JScrollPane jScrollPaneText;
    private javax.swing.JTable jTable;
    public javax.swing.JTextArea jTextArea;
    // End of variables declaration//GEN-END:variables

    public void updateTable(RequestEvent requestEvent, Request request, ServerTransaction transaction) {
        // Get the table model.
        DefaultTableModel tableModel = (DefaultTableModel) this.jTable.getModel();
        // Get the headers.
        FromHeader from = (FromHeader) request.getHeader("From");
        ToHeader to = (ToHeader) request.getHeader("To");
        CallIdHeader callId = (CallIdHeader) request.getHeader("Call-Id");
        CSeqHeader cSeq = (CSeqHeader) request.getHeader("CSeq");
        // Get the SIP dialog.
        Dialog dialog = transaction.getDialog();
        // Add a new line to the table.
        tableModel.addRow(new Object[]{
                (new Date()).toString(),
                request.getRequestURI() != null ? request.getRequestURI().toString() : "(unknown)",
                from != null ? from.getAddress() : "(unknown)",
                to != null ? to.getAddress() : "(unknown)",
                callId != null ? callId.getCallId() : "(unknown)",
                cSeq != null ? cSeq.getSeqNumber() + " " + cSeq.getMethod() : "(unknown)",
                dialog != null ? dialog.getDialogId() : "",
                transaction.getBranchId(),
                "Request",
                request.getMethod()});
    }
}
