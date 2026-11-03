package ar.edu.utn.tup.solicitudes.util;

public class NotificadorConsola {

    public void notificar(String email, String mensaje) {
        System.out.println("NOTIFICACION a " + email + ": " + mensaje);
    }
}
