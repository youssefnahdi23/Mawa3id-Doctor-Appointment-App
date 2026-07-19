import 'package:flutter/widgets.dart';

/// Root navigator key, so code outside the widget tree (e.g. a push-notification
/// tap handler) can navigate via go_router without a [BuildContext].
final rootNavigatorKey = GlobalKey<NavigatorState>();
