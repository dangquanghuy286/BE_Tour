
        package com.project.booktour.components;
        import org.springframework.stereotype.Component;
        import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

        import java.util.ArrayList;
        import java.util.List;
        import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterRegistry {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((throwable) -> emitters.remove(emitter));
    }

    public List<SseEmitter> getEmitters() {
        return new ArrayList<>(emitters);
    }
}