// Basic smoke test: the app boots and renders the splash screen without throwing.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:hangout_app/main.dart';
import 'package:hangout_app/core/constants/app_constants.dart';

void main() {
  testWidgets('App boots and shows the splash screen', (WidgetTester tester) async {
    await tester.pumpWidget(const HangOutApp());
    await tester.pump();

    expect(find.text(AppConstants.appName), findsOneWidget);
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
