package com.stepside.StepSide.auth.service;

import com.stepside.StepSide.auth.dto.CreateUserRequest;
import com.stepside.StepSide.auth.dto.CreateUserResponse;
//import com.stepside.StepSide.auth.dto.LoginRequest;
//import com.stepside.StepSide.auth.dto.LoginResponse;
//import com.stepside.StepSide.auth.dto.ForgotPasswordRequest;
//import com.stepside.StepSide.auth.dto.ResetPasswordRequest;

/**
 * Contrato de negocio transversal encargado de gobernar los flujos de seguridad,
 * registro corporativo y autenticación de la plataforma StepSide en MongoDB Atlas.
 * Saneado minuciosamente por Fabián para anular acoplamientos relacionales.
 */
public interface AuthService {

    /**
     * Procesa el registro atómico y compuesto de una nueva cuenta en el sistema.
     * Persiste de forma secuencial la Empresa, la Persona vinculada con su grafo NoSQL,
     * y las credenciales del usuario en estado pendiente de aprobación (PENDING_APPROVAL).
     *
     * @param request Payload jerárquico inmutable con los datos de cuenta, persona y compañía.
     * @return CreateUserResponse con los identificadores físicos String generados por MongoDB.
     */
    CreateUserResponse signUp(CreateUserRequest request);

    /**
     * Procesa la autenticación del usuario validando credenciales y estados de seguridad.
     *
     * @param request Payload inmutable con las credenciales de acceso.
     * @return LoginResponse con el token JWT emitido y estado actual de la cuenta.
     */
    //LoginResponse login(LoginRequest request);

    /**
     * Procesa la solicitud de recuperación de contraseña.
     * Valida la existencia del e-mail, genera un token de seguridad temporal,
     * y despacha la notificación dinámica basada en la plantilla 'PASSWORD_RESET'.
     *
     * @param request DTO inmutable con el correo del usuario que olvidó su clave.
     */
    //void forgotPassword(ForgotPasswordRequest request);

    /**
     * Procesa la ejecución del cambio definitivo de clave en el cluster cloud,
     * validando la vigencia del token e impactando el nuevo hash cifrado.
     */
    //void resetPassword(ResetPasswordRequest request);
}
