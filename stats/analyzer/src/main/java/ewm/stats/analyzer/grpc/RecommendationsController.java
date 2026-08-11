package ewm.stats.analyzer.grpc;

import ewm.stats.analyzer.service.AnalyzerService;
import ewm.stats.analyzer.service.RecommendedEvent;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsController
        extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final AnalyzerService analyzerService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос рекомендаций: userId={}, maxResults={}",
                    request.getUserId(), request.getMaxResults());

            List<RecommendedEvent> events = analyzerService.getRecommendationsForUser(
                    request.getUserId(), request.getMaxResults());

            sendAll(events, responseObserver);
        } catch (Exception e) {
            sendError(e, responseObserver);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос похожих мероприятий: eventId={}, userId={}, maxResults={}",
                    request.getEventId(), request.getUserId(), request.getMaxResults());

            List<RecommendedEvent> events = analyzerService.getSimilarEvents(
                    request.getEventId(), request.getUserId(), request.getMaxResults());

            sendAll(events, responseObserver);
        } catch (Exception e) {
            sendError(e, responseObserver);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос суммы взаимодействий: eventIds={}", request.getEventIdList());

            List<RecommendedEvent> events =
                    analyzerService.getInteractionsCount(request.getEventIdList());

            sendAll(events, responseObserver);
        } catch (Exception e) {
            sendError(e, responseObserver);
        }
    }

    private void sendAll(List<RecommendedEvent> events,
                         StreamObserver<RecommendedEventProto> responseObserver) {
        for (RecommendedEvent event : events) {
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(event.eventId())
                    .setScore(event.score())
                    .build());
        }
        responseObserver.onCompleted();
    }

    private void sendError(Exception e, StreamObserver<RecommendedEventProto> responseObserver) {
        log.error("Ошибка при обработке gRPC-запроса", e);
        responseObserver.onError(Status.INTERNAL
                .withDescription(e.getMessage())
                .withCause(e)
                .asRuntimeException());
    }
}