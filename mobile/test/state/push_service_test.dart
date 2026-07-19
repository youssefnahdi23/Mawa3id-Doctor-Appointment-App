import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/push/push_messaging.dart';
import 'package:mawa3id/core/push/push_service.dart';
import 'package:mawa3id/features/notifications/data/device_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDeviceRepository extends Mock implements DeviceRepository {}

/// Controllable fake for the FCM/local-notification seam.
class _FakeMessaging implements PushMessaging {
  bool granted = true;
  String? token = 'tok-1';
  PushMessage? initial;
  bool deleted = false;
  final List<PushMessage> shown = [];

  final _tokenRefresh = StreamController<String>.broadcast();
  final _foreground = StreamController<PushMessage>.broadcast();
  final _opened = StreamController<PushMessage>.broadcast();

  void emitTokenRefresh(String t) => _tokenRefresh.add(t);
  void emitForeground(PushMessage m) => _foreground.add(m);
  void emitOpened(PushMessage m) => _opened.add(m);

  @override
  Future<bool> requestPermission() async => granted;
  @override
  Future<String?> getToken() async => token;
  @override
  Stream<String> get onTokenRefresh => _tokenRefresh.stream;
  @override
  Stream<PushMessage> get onForegroundMessage => _foreground.stream;
  @override
  Stream<PushMessage> get onMessageOpenedApp => _opened.stream;
  @override
  Future<PushMessage?> getInitialMessage() async => initial;
  @override
  Future<void> deleteToken() async => deleted = true;
  @override
  Future<void> showNotification(PushMessage message) async => shown.add(message);
}

Future<void> _settle() => Future<void>.delayed(Duration.zero);

void main() {
  late _MockDeviceRepository repository;
  late _FakeMessaging messaging;

  setUp(() {
    repository = _MockDeviceRepository();
    messaging = _FakeMessaging();
    when(() => repository.register(
            token: any(named: 'token'), platform: any(named: 'platform')))
        .thenAnswer((_) async {});
    when(() => repository.unregister(any())).thenAnswer((_) async {});
  });

  PushService build({
    void Function(PushMessage)? onForeground,
    void Function(PushMessage)? onOpen,
  }) =>
      PushService(
        messaging: messaging,
        repository: repository,
        platform: 'ANDROID',
        onForeground: onForeground,
        onOpen: onOpen,
      );

  test('registerCurrentDevice registers the token with the platform', () async {
    await build().registerCurrentDevice();

    verify(() => repository.register(token: 'tok-1', platform: 'ANDROID'))
        .called(1);
  });

  test('does not register when permission is denied', () async {
    messaging.granted = false;

    await build().registerCurrentDevice();

    verifyNever(() => repository.register(
        token: any(named: 'token'), platform: any(named: 'platform')));
  });

  test('does not register when no token is available', () async {
    messaging.token = null;

    await build().registerCurrentDevice();

    verifyNever(() => repository.register(
        token: any(named: 'token'), platform: any(named: 'platform')));
  });

  test('a token refresh re-registers the new token', () async {
    final service = build();
    await service.registerCurrentDevice();
    clearInteractions(repository);

    messaging.emitTokenRefresh('tok-2');
    await _settle();

    verify(() => repository.register(token: 'tok-2', platform: 'ANDROID'))
        .called(1);
    service.dispose();
  });

  test('unregister removes the token from the server and the device', () async {
    final service = build();
    await service.registerCurrentDevice();

    await service.unregister();

    verify(() => repository.unregister('tok-1')).called(1);
    expect(messaging.deleted, isTrue);
  });

  test('a foreground message is shown locally and reported', () async {
    final foreground = <PushMessage>[];
    final service = build(onForeground: foreground.add);
    await service.registerCurrentDevice();

    messaging.emitForeground(const PushMessage(title: 'Hi', body: 'There'));
    await _settle();

    expect(messaging.shown, hasLength(1));
    expect(foreground.single.title, 'Hi');
    service.dispose();
  });

  test('an initial (terminated-tap) message routes via onOpen', () async {
    messaging.initial = const PushMessage(data: {'appointmentId': '42'});
    final opened = <PushMessage>[];

    await build(onOpen: opened.add).registerCurrentDevice();

    expect(opened.single.appointmentId, 42);
  });

  test('a background-tap message routes via onOpen', () async {
    final opened = <PushMessage>[];
    final service = build(onOpen: opened.add);
    await service.registerCurrentDevice();

    messaging.emitOpened(const PushMessage(data: {'type': 'APPOINTMENT_BOOKED'}));
    await _settle();

    expect(opened.single.type, 'APPOINTMENT_BOOKED');
    service.dispose();
  });
}
