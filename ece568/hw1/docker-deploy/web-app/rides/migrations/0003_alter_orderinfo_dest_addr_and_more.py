# Generated by Django 4.0.1 on 2022-01-26 17:30

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('rides', '0002_alter_orderinfo_arrival_date_ridesharer_rideowner'),
    ]

    operations = [
        migrations.AlterField(
            model_name='orderinfo',
            name='dest_addr',
            field=models.TextField(default=''),
        ),
        migrations.AlterField(
            model_name='orderinfo',
            name='driver_name',
            field=models.CharField(default='', max_length=50),
        ),
        migrations.AlterField(
            model_name='orderinfo',
            name='plate_num',
            field=models.CharField(default='', max_length=20),
        ),
        migrations.AlterField(
            model_name='orderinfo',
            name='username',
            field=models.CharField(default='', max_length=50),
        ),
    ]
