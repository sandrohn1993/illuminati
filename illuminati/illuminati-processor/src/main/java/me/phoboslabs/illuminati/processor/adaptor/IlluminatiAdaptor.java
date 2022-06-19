/*
 * Copyright 2017 Phoboslabs.me
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.phoboslabs.illuminati.processor.adaptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServletRequest;
import me.phoboslabs.illuminati.annotation.Illuminati;
import me.phoboslabs.illuminati.annotation.enums.PackageType;
import me.phoboslabs.illuminati.common.IlluminatiCommon;
import me.phoboslabs.illuminati.common.constant.IlluminatiConstant;
import me.phoboslabs.illuminati.common.dto.impl.IlluminatiDataInterfaceModelImpl;
import me.phoboslabs.illuminati.common.dto.impl.IlluminatiTemplateInterfaceModelImpl;
import me.phoboslabs.illuminati.common.properties.IlluminatiPropertiesHelper;
import me.phoboslabs.illuminati.common.util.StringObjectUtils;
import me.phoboslabs.illuminati.processor.executor.IlluminatiExecutor;
import me.phoboslabs.illuminati.processor.executor.impl.IlluminatiBackupExecutorImpl;
import me.phoboslabs.illuminati.processor.executor.impl.IlluminatiDataExecutorImpl;
import me.phoboslabs.illuminati.processor.executor.impl.IlluminatiTemplateExecutorImpl;
import me.phoboslabs.illuminati.processor.infra.restore.impl.RestoreTemplateData;
import me.phoboslabs.illuminati.processor.properties.IlluminatiPropertiesImpl;
import me.phoboslabs.illuminati.processor.shutdown.IlluminatiGracefulShutdownChecker;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 10/07/2017.
 */
public class IlluminatiAdaptor {

    private final Logger illuminatiInitLogger = LoggerFactory.getLogger(IlluminatiAdaptor.class);

    private static IlluminatiAdaptor ILLUMINATI_ADAPTOR_INSTANCE = new IlluminatiAdaptor();

    private static final AtomicInteger SAMPLING_RATE_CHECKER = new AtomicInteger(1);
    private static int SAMPLING_RATE = 20;
    private static final int CHAOS_BOMBER_NUMBER = (int) (Math.random() * 100) + 1;

    private static IlluminatiExecutor<IlluminatiDataInterfaceModelImpl> ILLUMINATI_DATA_EXECUTOR;
    private static IlluminatiExecutor<IlluminatiTemplateInterfaceModelImpl> ILLUMINATI_TEMPLATE_EXECUTOR;
    private static IlluminatiBackupExecutorImpl ILLUMINATI_BACKUP_EXECUTOR;

    private static RestoreTemplateData RESTORE_TEMPLATE_DATA;

    static {
        try {
            IlluminatiCommon.init();

            final String samplingRate = IlluminatiPropertiesHelper.getPropertiesValueByKey(IlluminatiPropertiesImpl.class,
                "illuminati", "samplingRate", "20");
            SAMPLING_RATE = StringObjectUtils.isValid(samplingRate) ? Integer.parseInt(samplingRate) : SAMPLING_RATE;

            if (IlluminatiConstant.ILLUMINATI_BACKUP_ACTIVATION) {
                ILLUMINATI_BACKUP_EXECUTOR = IlluminatiBackupExecutorImpl.getInstance().init();
            }

            ILLUMINATI_TEMPLATE_EXECUTOR = IlluminatiTemplateExecutorImpl.getInstance(ILLUMINATI_BACKUP_EXECUTOR).init();
            ILLUMINATI_DATA_EXECUTOR = IlluminatiDataExecutorImpl.getInstance(ILLUMINATI_TEMPLATE_EXECUTOR).init();

            if (IlluminatiConstant.ILLUMINATI_BACKUP_ACTIVATION) {
                RESTORE_TEMPLATE_DATA = RestoreTemplateData.getInstance(ILLUMINATI_TEMPLATE_EXECUTOR).init();
            }

            final String brokerType = IlluminatiPropertiesHelper.getPropertiesValueByKey(IlluminatiPropertiesImpl.class,
                "illuminati", "broker", "unknown");
            String clusterList = IlluminatiPropertiesHelper.getPropertiesValueByKey(IlluminatiPropertiesImpl.class, "illuminati",
                "clusterList", "unknown");
            if (brokerType.equalsIgnoreCase("simple")) {
                clusterList = "simple mode is don't need a cluster";
            }

            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            System.out.println("@ The illuminati is now activated.                             ");
            System.out.println("@ Broker Type : " + brokerType + "                                 ");
            System.out.println("@ Cluster List : " + clusterList + "                               ");
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        } catch (Exception ex) {
            System.out.println("################################################################");
            System.out.println("# The illuminati is not activated.                             #");
            System.out.println("################################################################");
            System.out.println("");
            System.out.println("The illuminati failed to initialize. check " + System.getProperty("spring.profiles.active")
                + " configuration files.");
            System.out.println("");
            System.out.println("Check the following message. ↓↓");
            System.out.println(ex.toString());
            System.out.println("");
        }
    }

    private IlluminatiAdaptor() {
    }

    public static IlluminatiAdaptor getInstance() {
        return ILLUMINATI_ADAPTOR_INSTANCE;
    }

    // ################################################################################################################
    // ### public methods                                                                                           ###
    // ################################################################################################################

    public boolean checkIlluminatiIsIgnore(ProceedingJoinPoint pjp) {
        try {
            final Illuminati illuminati = this.getIlluminatiAnnotation(pjp);
            return illuminati == null || illuminati.ignore();
        } catch (Exception ignore) {
        }
        return true;
    }

    public Object executeIlluminati(ProceedingJoinPoint pjp, HttpServletRequest request) throws Throwable {
        if (!this.checkConditionOfIlluminatiBasicExecution(pjp)) {
            return pjp.proceed();
        }

        if (!this.checkSamplingRate(pjp)) {
            this.illuminatiInitLogger.debug("ignore illuminati processor.");
            return pjp.proceed();
        }

        return addToQueue(pjp, request, false);
    }

    /**
     * it is only execute on debug mode and activated chaosBomber. can't be use sampling rate.
     *
     * @param pjp     - join point
     * @param request - request param (client)
     * @return method execute result
     * @throws Throwable - error origin object
     */
    public Object executeIlluminatiByChaosBomber(ProceedingJoinPoint pjp, HttpServletRequest request) throws Throwable {
        if (!this.checkConditionOfIlluminatiBasicExecution(pjp)) {
            return pjp.proceed();
        }

        return addToQueue(pjp, request, IlluminatiConstant.ILLUMINATI_DEBUG);
    }

    // ################################################################################################################
    // ### private methods                                                                                          ###
    // ################################################################################################################

    private boolean checkConditionOfIlluminatiBasicExecution(ProceedingJoinPoint pjp) {
        if (IlluminatiGracefulShutdownChecker.getIlluminatiReadyToShutdown()) {
            return false;
        }
        if (this.checkIgnoreProfile(pjp)) {
            return false;
        }

        return !this.isActivateIlluminatiSwitch() || this.isOnIlluminatiSwitch();
    }

    private boolean checkIgnoreProfile(ProceedingJoinPoint pjp) {
        boolean checkResult = false;
        try {
            Illuminati illuminati = this.getIlluminatiAnnotation(pjp);
            if (illuminati.ignoreProfile().length == 0) {
                return false;
            }

            final String activeProfileKeyword = illuminati.profileKeyword();
            final String activatedProfileKeyword = System.getProperty(activeProfileKeyword);
            checkResult = Arrays.stream(illuminati.ignoreProfile()).anyMatch(activatedProfileKeyword::equalsIgnoreCase);
        } catch (Exception ignore) {
        }

        return checkResult;
    }

    private Illuminati getIlluminatiAnnotation(ProceedingJoinPoint pjp) {
        final MethodSignature signature = (MethodSignature) pjp.getSignature();
        final Method method = signature.getMethod();

        Illuminati illuminati = method.getAnnotation(Illuminati.class);

        if (illuminati == null) {
            illuminati = pjp.getTarget().getClass().getAnnotation(Illuminati.class);
        }

        return illuminati;
    }

    private int getCustomSamplingRate(ProceedingJoinPoint pjp) {
        try {
            final Illuminati illuminati = this.getIlluminatiAnnotation(pjp);
            return illuminati != null ? illuminati.samplingRate() : 0;
        } catch (Exception ignore) {
        }
        return 0;
    }

    private boolean isActivateIlluminatiSwitch() {
        return IlluminatiConstant.ILLUMINATI_SWITCH_ACTIVATION;
    }

    private boolean isOnIlluminatiSwitch() {
        return IlluminatiConstant.ILLUMINATI_SWITCH_VALUE.get();
    }

    private Object addToQueue(ProceedingJoinPoint pjp, HttpServletRequest request, boolean isActiveChaosBomber) throws Throwable {
        final long start = System.currentTimeMillis();
        final Map<String, Object> originMethodExecute = getMethodExecuteResult(pjp);
        final long elapsedTime = System.currentTimeMillis() - start;

        Throwable throwable = null;
        if (originMethodExecute.containsKey("throwable")) {
            throwable = (Throwable) originMethodExecute.get("throwable");
        }

        if (isActiveChaosBomber && throwable == null && CHAOS_BOMBER_NUMBER == ((int) (Math.random() * 100) + 1)) {
            throwable = new Throwable("Illuminati ChaosBomber Exception Activate");
            request.setAttribute("ChaosBomber", "true");
        }

        final Illuminati illuminati = this.getIlluminatiAnnotation(pjp);
        final PackageType packageType;
        if (illuminati != null) {
            packageType = illuminati.packageType();
        } else {
            packageType = PackageType.DEFAULT;
        }

        ILLUMINATI_DATA_EXECUTOR.addToQueue(IlluminatiDataInterfaceModelImpl
            .Builder(request, (MethodSignature) pjp.getSignature(), pjp.getArgs(), elapsedTime, originMethodExecute)
            .setPackageType(packageType.getPackageType()));

        if (throwable != null) {
            throw throwable;
        }

        return originMethodExecute.get("result");
    }

    private boolean checkSamplingRate(ProceedingJoinPoint pjp) {
        int customSamplingRate = this.getCustomSamplingRate(pjp);
        if (customSamplingRate == 0) {
            customSamplingRate = SAMPLING_RATE;
        }

        //SAMPLING_RATE_CHECKER.compareAndSet(100, 1);

        // sometimes compareAndSet does not work.
        // So add this code. This code forces a reset to 1 if greater than 100.
        if (SAMPLING_RATE_CHECKER.get() > 100) {
            SAMPLING_RATE_CHECKER.set(1);
            return true;
        }

        return SAMPLING_RATE_CHECKER.getAndIncrement() <= customSamplingRate;
    }

    private Map<String, Object> getMethodExecuteResult(ProceedingJoinPoint pjp) {
        final Map<String, Object> originMethodExecute = new HashMap<>();

        try {
            originMethodExecute.put("result", pjp.proceed());
        } catch (Throwable ex) {
            originMethodExecute.put("throwable", ex);
            this.illuminatiInitLogger.error("error : check your process. ({})", ex.toString(), ex);
            originMethodExecute.put("result", StringObjectUtils.getExceptionMessageChain(ex));
        }

        return originMethodExecute;
    }
}
