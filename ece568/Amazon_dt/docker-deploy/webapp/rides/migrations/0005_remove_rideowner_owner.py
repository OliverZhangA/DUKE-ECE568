# Generated by Django 4.0.1 on 2022-01-26 19:44

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('rides', '0004_rename_ride_order_rideowner_orderinfo'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='rideowner',
            name='owner',
        ),
    ]